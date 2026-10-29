package com.music.bitchord.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.klyra.shared.*
import com.music.bitchord.data.model.ShelfItem
import java.util.UUID

@Composable
fun KlyraLibrarySection(onPlay: (ShelfItem) -> Unit) {
    val state by KlyraAccount.state.collectAsState()
    val tracks = remember(state.records) { state.records.filter { it.kind == "track" }.mapNotNull { runCatching { JsonCodec.decodeFromString<Track>(it.payload) }.getOrNull() }.associateBy { it.id } }
    val favorites = state.records.filter { it.kind == "like" }.mapNotNull { tracks[it.payload] }
    val playlists = state.records.filter { it.kind == "playlist" }.mapNotNull { runCatching { JsonCodec.decodeFromString<Playlist>(it.payload) }.getOrNull() }
    var create by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var opened by remember { mutableStateOf<String?>(null) }
    var rename by remember { mutableStateOf<Playlist?>(null) }
    var remove by remember { mutableStateOf<Playlist?>(null) }
    var newName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Your Klyra library", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            TextButton({ create = !create }) { Text("New playlist") }
        }
        if (create) {
            OutlinedTextField(title, { title = it }, label = { Text("Playlist name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button({ KlyraAccount.savePlaylist(Playlist(UUID.randomUUID().toString(), title.trim()), emptyList()); title = ""; create = false }, enabled = title.isNotBlank() && !state.busy) { Text("Create playlist") }
        }
        Text(if (state.email == null) "Saved on this device. Sign in in Settings to sync." else "Your account library · ${state.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        playlists.forEach { playlist ->
            TextButton({ opened = if (opened == playlist.id) null else playlist.id }) { Text("${playlist.name} · ${playlist.trackIds.size} songs") }
            if (opened == playlist.id) {
                Row {
                    TextButton({ rename = playlist; newName = playlist.name }) { Text("Rename") }
                    TextButton({ remove = playlist }) { Text("Delete playlist") }
                }
                if (playlist.trackIds.isEmpty()) Text("Add songs from Favorites below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                playlist.trackIds.mapNotNull(tracks::get).forEach { track ->
                KlyraTrackRow(track, onPlay) {
                    val index = playlist.trackIds.indexOf(track.id)
                    TextButton({
                        val ids = playlist.trackIds.toMutableList()
                        java.util.Collections.swap(ids, index, index - 1)
                        KlyraAccount.savePlaylist(playlist.copy(trackIds = ids), tracks.values.toList())
                    }, enabled = index > 0 && !state.busy) { Text("↑") }
                    TextButton({ KlyraAccount.savePlaylist(playlist.copy(trackIds = playlist.trackIds - track.id), tracks.values.toList()) }, enabled = !state.busy) { Text("Remove") }
                }
                }
            }
        }
        Text("Favorites", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (favorites.isEmpty()) Text("Tap the heart in Now Playing to keep a song here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        favorites.forEach { track ->
            var menu by remember(track.id) { mutableStateOf(false) }
            KlyraTrackRow(track, onPlay) {
                Box {
                    TextButton({ menu = true }) { Text("Add to…") }
                    DropdownMenu(menu, { menu = false }) {
                        if (playlists.isEmpty()) DropdownMenuItem(text = { Text("Create a playlist first") }, onClick = { menu = false; create = true })
                        playlists.forEach { playlist ->
                            DropdownMenuItem(text = { Text(playlist.name) }, onClick = {
                                KlyraAccount.savePlaylist(playlist.copy(trackIds = (playlist.trackIds + track.id).distinct()), tracks.values.toList()); menu = false
                            })
                        }
                    }
                }
            }
        }
        state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    rename?.let { playlist -> AlertDialog(onDismissRequest = { rename = null }, title = { Text("Rename playlist") },
        text = { OutlinedTextField(newName, { newName = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton({ KlyraAccount.savePlaylist(playlist.copy(name = newName.trim()), tracks.values.toList()); rename = null }, enabled = newName.isNotBlank() && !state.busy) { Text("Save") } },
        dismissButton = { TextButton({ rename = null }) { Text("Cancel") } }) }
    remove?.let { playlist -> AlertDialog(onDismissRequest = { remove = null }, title = { Text("Delete ${playlist.name}?") },
        text = { Text("This removes the playlist from your Klyra library on synced devices. Songs stay in Favorites.") },
        confirmButton = { TextButton({ KlyraAccount.deletePlaylist(playlist.id); remove = null }, enabled = !state.busy) { Text("Delete") } },
        dismissButton = { TextButton({ remove = null }) { Text("Cancel") } }) }
}

@Composable
private fun KlyraTrackRow(track: Track, onPlay: (ShelfItem) -> Unit, actions: @Composable () -> Unit) {
    val supported = track.source == Source.YOUTUBE
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.weight(1f).clickable(enabled = supported) { onPlay(ShelfItem(track.title, track.artist, track.artwork, track.sourceId, null)) }) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(track.artist + if (!supported) " · Open in original Klyra source" else "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}
