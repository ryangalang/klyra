package com.music.bitchord.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Product branding stays separate from the retained, offline-readable legal notices. */
@Composable
internal fun KlyraAbout(version: String) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showFullLicense by rememberSaveable { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
    ) {
        Text("Klyra $version", style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = { showAbout = true }) { Text("About") }
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("Klyra") },
            text = {
                Column {
                    Text("Every song, a little closer.")
                    Text("Version $version", modifier = Modifier.padding(top = 12.dp))
                    TextButton(onClick = {
                        showAbout = false
                        showLicenses = true
                        showFullLicense = false
                    }) { Text("Open-source licenses") }
                }
            },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Close") } },
        )
    }
    if (showLicenses) {
        val context = LocalContext.current
        val asset = if (showFullLicense) "klyra_gpl.txt" else "klyra_notices.txt"
        val legalText by produceState("Loading…", asset) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open(asset).bufferedReader().use { it.readText() }
                }.getOrElse { "Unable to read the bundled notice. See LICENSE in the matching source archive." }
            }
        }
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(if (showFullLicense) "GNU GPL version 3" else "Open-source licenses") },
            text = {
                SelectionContainer {
                    Text(
                        legalText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullLicense = !showFullLicense }) {
                    Text(if (showFullLicense) "Attribution" else "Full GPL license")
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } },
        )
    }
}
