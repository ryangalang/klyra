package com.music.bitchord.desktop

import app.klyra.shared.KlyraReleases
import app.klyra.shared.HttpApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DesktopUpdateChecker {
    data class UpdateInfo(val version: String, val releaseUrl: String, val downloadUrl: String?, val notes: String?, val asset: KlyraReleases.Asset)
    val currentVersion: String = System.getProperty("bitchord.version") ?: "1.8-klyra.0"
    internal val client = HttpApi().client
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val release = KlyraReleases.fetch(client) ?: return@runCatching null
            if (!KlyraReleases.newer(release.version, currentVersion)) return@runCatching null
            val asset = release.assets.firstOrNull { it.name == "Klyra-Windows-x64-setup.exe" } ?: return@runCatching null
            UpdateInfo(release.version, KlyraReleases.PAGE, asset.url, release.notes, asset)
        }.getOrNull()
    }
}
