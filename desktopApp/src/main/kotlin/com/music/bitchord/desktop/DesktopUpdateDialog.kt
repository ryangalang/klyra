package com.music.bitchord.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.klyra.shared.KlyraReleases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun DesktopUpdateDialog(update: DesktopUpdateChecker.UpdateInfo, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var progress by remember(update.version) { mutableStateOf<Float?>(null) }
    var ready by remember(update.version) { mutableStateOf<File?>(null) }
    var failure by remember(update.version) { mutableStateOf<String?>(null) }
    val cancelled = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    DisposableEffect(Unit) { onDispose { cancelled.set(true) } }
    AlertDialog(onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text("Klyra ${update.version} is available") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Installed: ${DesktopUpdateChecker.currentVersion}. Your library and settings stay on this computer.")
            update.notes?.takeIf { it.isNotBlank() }?.let { Text(it.take(500)) }
            progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()); Text("Downloading ${(it * 100).toInt()}%") }
            if (ready != null) Text("Download verified. Install update will close Klyra and open Windows Setup.")
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = progress == null, onClick = {
            if (ready != null) {
                runCatching { ProcessBuilder(ready!!.absolutePath).start() }
                    .onSuccess { kotlin.system.exitProcess(0) }
                    .onFailure { failure = "Could not open Setup: ${it.message}" }
            } else scope.launch {
                progress = 0f; failure = null; cancelled.set(false)
                val result = withContext(Dispatchers.IO) { runCatching {
                    val folder = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "KLYRA Preview/updates")
                    val target = File(folder, "Klyra-${update.version}-setup.exe")
                    KlyraReleases.download(DesktopUpdateChecker.client, update.asset, target, cancelled::get) { progress = it }
                    target
                } }
                progress = null
                result.onSuccess { ready = it }.onFailure { failure = it.message ?: "Download failed. Try again." }
            }
        }) { Text(if (ready != null) "Install update" else if (failure != null) "Retry download" else "Download update") } },
        dismissButton = { TextButton(onClick = { cancelled.set(true); onDismiss() }) { Text(if (progress == null) "Later" else "Cancel download") } })
}
