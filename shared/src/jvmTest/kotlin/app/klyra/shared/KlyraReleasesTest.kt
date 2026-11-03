package app.klyra.shared

import kotlin.test.*

class KlyraReleasesTest {
    private fun release(name: String = "Klyra-Android-arm64.apk", digest: String = "a".repeat(64), host: String = "https://github.com/ryangalang/klyra/releases/download/v1.8-klyra.8/") =
        """{"tag_name":"v1.8-klyra.8","assets":[{"name":"$name","state":"uploaded","size":42,"digest":"sha256:$digest","browser_download_url":"$host$name"}]}"""
    @Test fun comparesKlyraRevisionsNumerically() {
        assertTrue(KlyraReleases.newer("1.8-klyra.10", "1.8-klyra.9"))
        assertFalse(KlyraReleases.newer("1.8-klyra.7", "1.8-klyra.7"))
        assertFalse(KlyraReleases.newer("1.8-klyra.6", "1.8-klyra.7"))
    }
    @Test fun choosesCompatibleApkOnly() {
        val r = KlyraReleases.parse(release())!!
        assertNotNull(KlyraReleases.androidAsset(r, listOf("arm64-v8a")))
        assertNull(KlyraReleases.androidAsset(r, listOf("armeabi-v7a")))
    }
    @Test fun permitsUniversalFallback() {
        assertNotNull(KlyraReleases.androidAsset(KlyraReleases.parse(release("Klyra-Android-universal.apk"))!!, listOf("arm64-v8a")))
    }
    @Test fun rejectsMissingDigestAndForeignDownloads() {
        assertTrue(KlyraReleases.parse(release(digest = ""))!!.assets.isEmpty())
        assertTrue(KlyraReleases.parse(release(host = "https://example.test/"))!!.assets.isEmpty())
    }
    @Test fun ignoresDraftsAndUpstreamVersionTags() {
        assertNull(KlyraReleases.parse(release().replace("{", "{\"draft\":true,", ignoreCase = false)))
        assertNull(KlyraReleases.parse(release().replace("v1.8-klyra.8", "v1.9-beta1")))
    }
    @Test fun downloadRejectsTamperedBytesAndNeverLeavesInstallableFile() {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").body(okhttp3.ResponseBody.create(null, "bad")).build()
        }.build()
        val dir = java.nio.file.Files.createTempDirectory("klyra-updater-test").toFile()
        val target = java.io.File(dir, "update.exe")
        try {
            assertFailsWith<IllegalStateException> {
                KlyraReleases.download(client, KlyraReleases.Asset("update.exe", "https://example.test/update", "a".repeat(64), 3), target) { }
            }
            assertFalse(target.exists())
            assertFalse(java.io.File(dir, "update.exe.part").exists())
        } finally { dir.deleteRecursively() }
    }
    @Test fun verifiedDownloadPreservesExactBytes() {
        val content = "verified fixture".toByteArray()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").body(okhttp3.ResponseBody.create(null, content)).build()
        }.build()
        val dir = java.nio.file.Files.createTempDirectory("klyra-updater-test").toFile()
        val target = java.io.File(dir, "update.exe")
        try {
            KlyraReleases.download(client, KlyraReleases.Asset("update.exe", "https://example.test/update", hash, content.size.toLong()), target) { }
            assertContentEquals(content, target.readBytes())
        } finally { dir.deleteRecursively() }
    }

}
