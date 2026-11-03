package com.music.bitchord.desktop

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.ui.icons.BitChordIcons
import kotlinx.coroutines.yield

/** Desktop navigation stays available as an icon rail when the window gets narrower. */
@Composable
internal fun DesktopSidebar(
    compact: Boolean,
    destination: DesktopDestination,
    settingsOpen: Boolean,
    accountPlaylists: List<ShelfItem>,
    localPlaylists: List<DesktopPlaylist>,
    openedCollectionId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDestinationSelected: (DesktopDestination) -> Unit,
    onOpenAccountPlaylist: (ShelfItem) -> Unit,
    onOpenLocalPlaylist: (DesktopPlaylist) -> Unit,
    onOpenSettings: () -> Unit,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
) {
    val searchFocusRequester = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    var restoreSearchFocus by remember { mutableStateOf(false) }
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(focusSearch, compact) {
        if (focusSearch && !compact) {
            runCatching { searchFocusRequester.requestFocus() }
            onSearchFocused()
        }
    }
    LaunchedEffect(windowInfo.isWindowFocused, compact) {
        if (!windowInfo.isWindowFocused) restoreSearchFocus = searchFocused
        else if (restoreSearchFocus && !compact) {
            yield()
            runCatching { searchFocusRequester.requestFocus() }
            restoreSearchFocus = false
        }
    }
    val shape = RoundedCornerShape(18.dp)
    val reduced by DesktopAppearanceSettings.reduceAnimation.collectAsState()
    val sidebarWidth by animateDpAsState(if (compact) 76.dp else 252.dp,
        animationSpec = tween(if (reduced) 0 else 180), label = "sidebarWidth")
    val queued by DesktopDownloadQueue.active.collectAsState()
    Column(
        Modifier.width(sidebarWidth).fillMaxHeight()
            .padding(start = 10.dp, top = 12.dp, end = 6.dp, bottom = 12.dp)
            .clip(shape).desktopBarGlass(shape).padding(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).desktopRowHover()
                    .clickable(role = Role.Button, onClickLabel = if (compact) "Expand sidebar" else "Collapse sidebar") {
                        DesktopSidebarSettings.set(if (compact) DesktopSidebarMode.EXPANDED else DesktopSidebarMode.COLLAPSED)
                    },
                contentAlignment = Alignment.Center,
            ) {
                val ink = MaterialTheme.colorScheme.onSurface
                Canvas(Modifier.size(22.dp).semantics { contentDescription = if (compact) "Expand sidebar" else "Collapse sidebar" }) {
                    val stroke = 1.6.dp.toPx()
                    drawRoundRect(ink, topLeft = Offset(stroke, stroke), size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2),
                        cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(stroke))
                    drawLine(ink, Offset(size.width * .36f, stroke), Offset(size.width * .36f, size.height - stroke), stroke)
                }
            }
            if (!compact) Text("Klyra", Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onBackground,
                fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
        }
        if (!compact) {
            DesktopSearchField(query, onQueryChange, onSearch,
                focusRequester = searchFocusRequester, onFocusChanged = { searchFocused = it },
                placeholder = "Search music")
            Spacer(Modifier.height(12.dp))
        }
        // Keep the main destinations anchored; only the library and playlist list scrolls.
        val primary = listOf(
                Triple(DesktopDestination.LISTEN_NOW, BitChordIcons.Home, "Home"),
                Triple(DesktopDestination.EXPLORE, BitChordIcons.Explore, "Explore"),
            ) + if (compact) listOf(Triple(DesktopDestination.SEARCH, BitChordIcons.Search, "Search")) else emptyList()
        primary.forEach { (target, icon, label) ->
                NavigationItem(icon, label, !settingsOpen && destination == target, compact) { onDestinationSelected(target) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            item { NavigationSection("YOUR LIBRARY", compact) }
            item { NavigationItem(BitChordIcons.Library, "Library", !settingsOpen && destination == DesktopDestination.LIBRARY, compact) { onDestinationSelected(DesktopDestination.LIBRARY) } }
            item { NavigationItem(BitChordIcons.Clock, "History", !settingsOpen && destination == DesktopDestination.HISTORY, compact) { onDestinationSelected(DesktopDestination.HISTORY) } }
            item { NavigationItem(BitChordIcons.Download, if (queued.isEmpty() || compact) "Downloads" else "Downloads · ${queued.size}", !settingsOpen && destination == DesktopDestination.DOWNLOADS, compact) { onDestinationSelected(DesktopDestination.DOWNLOADS) } }
            item { NavigationItem(Icons.Rounded.Folder, "Local", !settingsOpen && destination == DesktopDestination.LOCAL_MUSIC, compact) { onDestinationSelected(DesktopDestination.LOCAL_MUSIC) } }
            if (!compact) {
                item { NavigationSection("PLAYLISTS · ${accountPlaylists.size + localPlaylists.size}", false) }
                itemsIndexed(accountPlaylists, key = { index, item -> "account:$index:${item.browseId}" }) { _, playlist ->
                    PlaylistItem(playlist.title, "Music library", playlist.thumbnailUrl, !settingsOpen && openedCollectionId == playlist.browseId) { onOpenAccountPlaylist(playlist) }
                }
                itemsIndexed(localPlaylists, key = { _, item -> "local:${item.id}" }) { _, playlist ->
                    PlaylistItem(playlist.title, "${playlist.songs.size} songs", playlist.songs.firstOrNull()?.thumbnailUrl, !settingsOpen && openedCollectionId == playlist.id) { onOpenLocalPlaylist(playlist) }
                }
                if (accountPlaylists.isEmpty() && localPlaylists.isEmpty()) item {
                    Text("Your saved playlists appear here.", Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall, color = DesktopSecondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .15f))
        Spacer(Modifier.height(8.dp))
        NavigationItem(Icons.Rounded.Settings, "Settings", settingsOpen, compact, onOpenSettings)
    }

}

@Composable
private fun NavigationSection(title: String, compact: Boolean) {
    if (compact) HorizontalDivider(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), color = Color.White.copy(alpha = .1f))
    else Text(title, Modifier.padding(start = 10.dp, top = 20.dp, bottom = 9.dp),
        color = DesktopSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}

@Composable
private fun NavigationItem(icon: ImageVector, label: String, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val ink = if (selected) MaterialTheme.colorScheme.onSurface else DesktopSecondary
    val modifier = Modifier.fillMaxWidth().clip(shape)
        .background(if (selected) Color.White.copy(alpha = .11f) else Color.Transparent)
        .desktopRowHover().selectable(selected, role = Role.Tab, onClick = onClick)
        .padding(horizontal = if (compact) 2.dp else 12.dp, vertical = 11.dp)
    if (compact) Box(modifier, contentAlignment = Alignment.Center) {
        Icon(icon, label, Modifier.size(21.dp), tint = ink)
    } else Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(21.dp), tint = ink)
        Spacer(Modifier.width(14.dp))
        Text(label, color = ink, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistItem(title: String, subtitle: String, artwork: String?, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(if (selected) Color.White.copy(alpha = .1f) else Color.Transparent)
        .desktopRowHover().selectable(selected, role = Role.Tab, onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (artwork != null) DesktopArtwork(artwork, Modifier.size(40.dp).clip(RoundedCornerShape(7.dp)), px = 96)
        else Box(Modifier.size(40.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = .07f)), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, tint = DesktopSecondary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = DesktopSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
