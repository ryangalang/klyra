package com.music.bitchord.desktop

import com.sun.jna.platform.win32.Crypt32Util
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** KLYRA: secrets are bound to the signed-in Windows user by DPAPI. No plaintext fallback. */
internal class WindowsCredentialStore(private val directory: Path) {
    internal fun pathFor(attributes: Map<String, String>): Path {
        val canonical = ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { out ->
                attributes.toSortedMap().forEach { (key, value) ->
                    out.writeUTF(key)
                    out.writeUTF(value)
                }
            }
        }.toByteArray()
        val id = MessageDigest.getInstance("SHA-256").digest(canonical)
            .joinToString("") { "%02x".format(it) }
        return directory.resolve("$id.dpapi")
    }

    @Synchronized
    fun store(attributes: Map<String, String>, secret: ByteArray): Boolean = runCatching {
        // Encrypt before writing any bytes, including to temporary files.
        val protected = Crypt32Util.cryptProtectData(secret)
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, ".credential-", ".tmp")
        try {
            Files.write(temporary, protected)
            Files.move(temporary, pathFor(attributes), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
        true
    }.getOrDefault(false)

    @Synchronized
    fun lookup(attributes: Map<String, String>): ByteArray? = runCatching {
        val path = pathFor(attributes)
        if (!Files.isRegularFile(path)) return null
        Crypt32Util.cryptUnprotectData(Files.readAllBytes(path))
    }.getOrNull()

    @Synchronized
    fun remove(attributes: Map<String, String>): Boolean = runCatching {
        Files.deleteIfExists(pathFor(attributes))
        true
    }.getOrDefault(false)

    fun available(): Boolean = runCatching {
        val probe = "KLYRA DPAPI availability".toByteArray()
        Crypt32Util.cryptUnprotectData(Crypt32Util.cryptProtectData(probe)).contentEquals(probe)
    }.getOrDefault(false)
}
