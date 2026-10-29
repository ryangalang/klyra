package app.klyra.shared

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

data class KlyraAccountState(
    val configured: Boolean = false,
    val email: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val records: List<CloudRecord> = emptyList(),
    val conflicts: Set<String> = emptySet(),
    val recovery: Boolean = false,
)

/** Original KLYRA Supabase protocol, isolated from the music provider's Google session. */
object KlyraAccount {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(KlyraAccountState())
    val state = mutable.asStateFlow()
    private var cloud: SupabaseClient? = null
    private lateinit var directory: File
    @Serializable private data class Library(val records: List<CloudRecord> = emptyList(), val pending: Set<String> = emptySet())
    private var library = Library()
    private var conflicts = emptySet<String>()
    private var recovery = false

    fun install(url: String, key: String, secrets: SecretStore, files: File) {
        require(!key.startsWith("sb_secret_")) { "Use a Supabase public/publishable key, never a secret key" }
        val jwtRole = runCatching {
            val payload = java.util.Base64.getUrlDecoder().decode(key.split('.').getOrNull(1).orEmpty())
            JsonCodec.parseToJsonElement(payload.toString(Charsets.UTF_8)).toString()
        }.getOrDefault("")
        require(!jwtRole.contains("service_role")) { "Service-role keys must never be bundled in an app" }
        directory = files
        cloud = SupabaseClient(HttpApi(), url.trim(), key.trim(), secrets)
        library = read()
        publish()
    }

    private fun file(): File {
        val owner = cloud?.session?.userId ?: "guest"
        val hash = MessageDigest.getInstance("SHA-256").digest(owner.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(directory, "$hash.json")
    }
    private fun read(): Library = runCatching { JsonCodec.decodeFromString<Library>(file().readText()) }.getOrDefault(Library())
    private fun save() {
        directory.mkdirs()
        val target = file().toPath()
        val temp = java.nio.file.Files.createTempFile(directory.toPath(), "library-", ".tmp")
        try {
            java.nio.file.Files.write(temp, JsonCodec.encodeToString(library).toByteArray(Charsets.UTF_8))
            java.nio.file.Files.move(temp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally { java.nio.file.Files.deleteIfExists(temp) }
    }
    private fun publish(message: String? = null) {
        mutable.value = KlyraAccountState(cloud?.configured == true, cloud?.session?.email,
            message = message, records = library.records.filterNot { it.deleted }, conflicts = conflicts, recovery = recovery)
    }
    private fun action(block: suspend () -> String?) { scope.launch {
        mutex.withLock {
            mutable.value = mutable.value.copy(busy = true, message = null)
            val result = runCatching { block() }
            publish(result.fold({ it }, { it.message ?: "Request failed. Please try again." }))
        }
    } }
    fun signIn(email: String, password: String) = action {
        checkNotNull(cloud).signIn(email, password)
        library = read() // Never combine guest/another user's records automatically.
        conflicts = emptySet()
        recovery = false
        syncNow()
    }
    fun signUp(email: String, password: String) = action {
        checkNotNull(cloud).signUp(email, password); "Check your email to confirm your KLYRA account."
    }
    fun recover(email: String) = action {
        checkNotNull(cloud).recover(email); "Copy the recovery link from your email and paste it into Verify email link below."
    }
    fun verifyEmailLink(link: String, email: String) = action {
        recovery = checkNotNull(cloud).verifyEmailLink(link, email)
        library = read()
        conflicts = emptySet()
        if (recovery) "Email verified. Choose a new password below." else syncNow()
    }
    fun updatePassword(password: String) = action {
        checkNotNull(cloud).updatePassword(password); recovery = false; "Password updated."
    }
    fun deleteAccount() = action {
        val ownerFile = file()
        checkNotNull(cloud).deleteAccount()
        ownerFile.delete()
        library = read(); conflicts = emptySet(); recovery = false
        "Your KLYRA account and cloud library were deleted."
    }
    fun signOut() = action {
        try { checkNotNull(cloud).signOut() } finally { library = read(); conflicts = emptySet(); recovery = false }
        "Signed out. Your cloud library remains in your account."
    }
    fun sync() = action { syncNow() }
    private suspend fun syncNow(): String {
        val api = checkNotNull(cloud)
        check(api.session != null) { "Sign in to sync your KLYRA library." }
        val initial = mergeLibrary(library.records, library.pending, api.pull())
        library = Library(initial.records, initial.pending)
        conflicts = initial.conflicts
        save()
        for (record in library.records.filter { it.id in library.pending }) {
            if (record.id in conflicts) continue
            val accepted = api.push(record)
            if (accepted) {
                library = library.copy(records = library.records.map { if (it.id == record.id) it.copy(revision = it.revision + 1) else it }, pending = library.pending - record.id)
                save() // Persist every acknowledgement, including when a later request fails.
            }
        }
        val merged = mergeLibrary(library.records, library.pending, api.pull())
        library = Library(merged.records, merged.pending)
        conflicts = merged.conflicts
        save()
        return if (conflicts.isEmpty()) "Library synced." else "${conflicts.size} edits conflict with another device. Choose which version to keep."
    }
    fun resolveConflicts(keepLocal: Boolean) = action {
        val remote = checkNotNull(cloud).pull().associateBy { it.id }
        val unresolved = conflicts
        library = library.copy(records = library.records.mapNotNull { record ->
            if (record.id !in unresolved) record else if (keepLocal) record.copy(revision = remote[record.id]?.revision ?: 0) else remote[record.id]
        }, pending = if (keepLocal) library.pending else library.pending - unresolved)
        conflicts = emptySet(); save(); syncNow()
    }
    fun deletePlaylist(id: String) = action {
        val recordId = "playlist:$id"
        library = library.copy(records = library.records.map { if (it.id == recordId) it.copy(deleted = true) else it }, pending = library.pending + recordId)
        save()
        if (cloud?.session != null) syncNow() else "Playlist removed on this device."
    }
    fun saveTrack(track: Track, liked: Boolean = true) = action {
        val safe = track.cloudSafe()
        val id = "like:${track.id}"
        val current = library.records.associateBy { it.id }.toMutableMap()
        current[track.id] = CloudRecord(track.id, "track", JsonCodec.encodeToString(safe), current[track.id]?.revision ?: 0)
        current[id] = CloudRecord(id, "like", track.id, current[id]?.revision ?: 0, !liked)
        // Device paths and local media are never sent to another device.
        val pending = if (track.source == Source.LOCAL) library.pending else library.pending + setOf(track.id, id)
        library = Library(current.values.toList(), pending)
        save()
        if (cloud?.session != null) syncNow() else "Saved on this device. Sign in to sync."
    }
    fun savePlaylist(playlist: Playlist, tracks: List<Track>) = action {
        val current = library.records.associateBy { it.id }.toMutableMap()
        val eligible = tracks.filter { it.source != Source.LOCAL }.map { it.cloudSafe() }
        eligible.forEach { current[it.id] = CloudRecord(it.id, "track", JsonCodec.encodeToString(it), current[it.id]?.revision ?: 0) }
        val id = "playlist:${playlist.id}"
        val safe = playlist.copy(trackIds = playlist.trackIds.filterNot { it.startsWith("local:") }, cover = playlist.cover?.takeIf { it.startsWith("https://") })
        current[id] = CloudRecord(id, "playlist", JsonCodec.encodeToString(safe), current[id]?.revision ?: 0)
        library = Library(current.values.toList(), library.pending + eligible.map { it.id } + id)
        save()
        if (cloud?.session != null) syncNow() else "Playlist saved on this device. Sign in to sync."
    }
}
