package app.klyra.shared

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import okhttp3.HttpUrl.Companion.toHttpUrl

interface SecretStore { fun read(key: String): String?; fun write(key: String, value: String?) }

class SupabaseClient(private val api: HttpApi, val url: String, private val publicKey: String, private val secrets: SecretStore) {
    val configured get() = url.startsWith("https://") && publicKey.isNotBlank()
    private val lock = Mutex()
    private var current: Session? = secrets.read("session")?.let { runCatching { JsonCodec.decodeFromString<Session>(it) }.getOrNull() }
    val session get() = current
    private fun save(session: Session?) { secrets.write("session", session?.let { JsonCodec.encodeToString(it) }); current = session }
    private fun parse(o: JsonObject): Session {
        val user = o["user"]!!.jsonObject
        return Session(o.string("access_token"), o.string("refresh_token"), user.string("id"), user.string("email"), System.currentTimeMillis() + o.number("expires_in") * 1000)
    }
    private suspend fun auth(path: String, body: JsonObject): JsonObject {
        check(configured) { "Cloud accounts are not configured in this build. You can still listen on this device." }
        return api.request("${url.trimEnd('/')}/auth/v1/$path", method = "POST", body = body, headers = mapOf("apikey" to publicKey)).jsonObject
    }
    suspend fun signIn(email: String, password: String): Session = lock.withLock {
        parse(auth("token?grant_type=password", buildJsonObject { put("email", email.trim()); put("password", password) })).also(::save)
    }
    suspend fun signUp(email: String, password: String) {
        require(password.length >= 8) { "Use at least 8 characters for your password." }
        auth("signup", buildJsonObject { put("email", email.trim()); put("password", password) })
    }
    suspend fun recover(email: String) { auth("recover?redirect_to=klyra%3A%2F%2Fauth%2Fcallback", buildJsonObject { put("email", email.trim()) }) }
    suspend fun resend(email: String) { auth("resend", buildJsonObject { put("type", "signup"); put("email", email.trim()) }) }
    suspend fun accessToken(): String = lock.withLock {
        val old = current ?: error("Sign in to sync your library.")
        if (old.expiresAt > System.currentTimeMillis() + 60_000) old.accessToken
        else parse(auth("token?grant_type=refresh_token", buildJsonObject { put("refresh_token", old.refreshToken) })).also(::save).accessToken
    }
    suspend fun authorized(path: String, method: String = "GET", body: JsonElement? = null): JsonElement =
        api.request("${url.trimEnd('/')}/$path", method = method, body = body, headers = mapOf("apikey" to publicKey, "Authorization" to "Bearer ${accessToken()}"))
    suspend fun updatePassword(password: String) {
        require(password.length >= 8) { "Use at least 8 characters." }
        authorized("auth/v1/user", "PUT", buildJsonObject { put("password", password) })
    }
    suspend fun signOut() {
        try { if (current != null) authorized("auth/v1/logout", "POST") } finally { save(null) }
    }
    fun googleUrl(): String {
        check(configured) { "Configure Supabase to use Google sign-in." }
        val verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
        secrets.write("pkce", verifier)
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        return "${url.trimEnd('/')}/auth/v1/authorize".toHttpUrl().newBuilder().addQueryParameter("provider", "google")
            .addQueryParameter("redirect_to", "klyra://auth/callback").addQueryParameter("code_challenge", challenge).addQueryParameter("code_challenge_method", "s256").build().toString()
    }
    suspend fun handleCallback(code: String): Session = lock.withLock {
        val verifier = secrets.read("pkce") ?: error("This sign-in link was not started on this device. Please try signing in again.")
        parse(auth("token?grant_type=pkce", buildJsonObject { put("auth_code", code); put("code_verifier", verifier) })).also { save(it); secrets.write("pkce", null) }
    }
    suspend fun handleEmailToken(tokenHash: String, type: String): Boolean {
        require(type in setOf("signup", "recovery", "email"))
        val result = auth("verify", buildJsonObject { put("token_hash", tokenHash); put("type", type) })
        if (result.string("access_token").isNotEmpty()) save(parse(result))
        return type == "recovery"
    }
    /** Redeem the original email link inside either app without a desktop URI handler. */
    suspend fun verifyEmailLink(link: String, expectedEmail: String): Boolean = lock.withLock {
        val parsed = link.trim().toHttpUrl()
        val project = url.toHttpUrl()
        require(parsed.isHttps && parsed.host == project.host && parsed.encodedPath == "/auth/v1/verify") {
            "Copy the original confirmation or recovery link from your KLYRA email."
        }
        val type = parsed.queryParameter("type").orEmpty()
        require(type in setOf("signup", "recovery", "email")) { "This is not an account confirmation or recovery link." }
        val token = parsed.queryParameter("token_hash") ?: parsed.queryParameter("token")
        require(!token.isNullOrBlank()) { "The email link is missing its verification token." }
        val result = parse(auth("verify", buildJsonObject { put("token_hash", token); put("type", type) }))
        require(result.email.equals(expectedEmail.trim(), ignoreCase = true)) { "The link belongs to a different email address. Enter that address first." }
        save(result)
        type == "recovery"
    }
    suspend fun deleteAccount() { authorized("rest/v1/rpc/delete_own_account", "POST"); save(null) }
    suspend fun pull(): List<CloudRecord> {
        val result = mutableListOf<CloudRecord>(); var offset = 0
        do {
            val page = authorized("rest/v1/library_records?select=*&order=id&limit=500&offset=$offset").jsonArray
            result += page.map { val o = it.jsonObject; CloudRecord(o.string("id"), o.string("kind"), o.string("payload"), o.number("revision"), o.bool("deleted")) }
            offset += page.size
        } while (page.size == 500)
        return result
    }
    suspend fun push(record: CloudRecord): Boolean = authorized("rest/v1/rpc/apply_library_record", "POST", buildJsonObject {
        put("record_id", record.id); put("record_kind", record.kind); put("record_payload", record.payload)
        put("base_revision", record.revision); put("is_deleted", record.deleted)
    }).jsonPrimitive.boolean
}
