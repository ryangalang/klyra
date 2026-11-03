package app.klyra.shared

import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/** Only the Klyra release channel can supply executable updates. */
object KlyraReleases {
    const val API = "https://api.github.com/repos/ryangalang/klyra/releases/latest"
    const val PAGE = "https://github.com/ryangalang/klyra/releases"
    data class Asset(val name: String, val url: String, val sha256: String, val size: Long)
    data class Release(val version: String, val notes: String, val assets: List<Asset>)
    fun parse(text: String): Release? {
        val o = JsonCodec.parseToJsonElement(text).jsonObject
        if (o.bool("draft") || o.bool("prerelease")) return null
        val version = o.string("tag_name").removePrefix("v")
        if (!version.matches(Regex("[0-9]+\\.[0-9]+-klyra\\.[0-9]+"))) return null
        val assets = (o["assets"] as? JsonArray).orEmpty().mapNotNull { item ->
            val a = item.jsonObject
            val name = a.string("name")
            val url = a.string("browser_download_url")
            val hash = a.string("digest").removePrefix("sha256:")
            if (a.string("state") != "uploaded" || !hash.matches(Regex("[a-f0-9]{64}")) ||
                !url.startsWith("https://github.com/ryangalang/klyra/releases/download/") || a.number("size") <= 0) null
            else Asset(name, url, hash, a.number("size"))
        }
        return Release(version, o.string("body"), assets)
    }
    fun newer(latest: String, current: String): Boolean {
        fun parts(s: String) = Regex("[0-9]+").findAll(s.removePrefix("v")).map { it.value.toLongOrNull() ?: 0 }.toList()
        val a = parts(latest); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
    fun androidAsset(release: Release, abis: List<String>): Asset? {
        val arch = abis.firstOrNull()?.let { when (it) {
            "arm64-v8a" -> "arm64"; "armeabi-v7a" -> "arm32"; "x86_64" -> "x64-emulator"; else -> null
        } }
        return release.assets.firstOrNull { it.name == "Klyra-Android-$arch.apk" }
            ?: release.assets.firstOrNull { it.name == "Klyra-Android-universal.apk" }
    }
    fun fetch(client: OkHttpClient): Release? = client.newCall(Request.Builder().url(API)
        .header("Accept", "application/vnd.github+json").header("User-Agent", "Klyra-Updater").build()).execute().use {
        if (it.code == 404) null else { check(it.isSuccessful) { "Update check failed (${it.code})." }; parse(it.body?.string().orEmpty()) }
    }
    /** No executable is offered to the installer until length and SHA-256 match the release. */
    fun download(client: OkHttpClient, asset: Asset, target: File, cancelled: () -> Boolean = { false }, progress: (Float) -> Unit) {
        require(asset.sha256.matches(Regex("[a-f0-9]{64}")))
        val partial = File(target.parentFile, target.name + ".part")
        target.parentFile.mkdirs()
        try {
            val hash = MessageDigest.getInstance("SHA-256")
            var count = 0L
            client.newBuilder().callTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS).build().newCall(Request.Builder().url(asset.url).build()).execute().use { response ->
                check(response.isSuccessful) { "Update download failed (${response.code})." }
                check(response.request.url.isHttps) { "The update host must use HTTPS." }
                val body = response.body ?: error("Empty update")
                body.byteStream().use { input -> partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        check(!cancelled()) { "Download cancelled" }
                        val read = input.read(buffer); if (read < 0) break
                        count += read
                        check(count <= asset.size) { "Update size does not match the release." }
                        hash.update(buffer, 0, read); output.write(buffer, 0, read)
                        progress((count.toDouble() / asset.size).toFloat())
                    }
                } }
            }
            check(count == asset.size && hash.digest().joinToString("") { "%02x".format(it) } == asset.sha256) { "Update verification failed. Please download again." }
            java.nio.file.Files.move(partial.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } finally { partial.delete() }
    }
}
