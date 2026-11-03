package com.music.bitchord.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal enum class DesktopSidebarMode { AUTO, EXPANDED, COLLAPSED }

/** A single menu toggles the full library and compact rail; old hidden preferences recover automatically. */
internal object DesktopSidebarSettings {
    private const val KEY = "sidebar_mode"
    private val current = MutableStateFlow(runCatching {
        DesktopSidebarMode.valueOf(DesktopPersistence().string(KEY, "AUTO"))
    }.getOrDefault(DesktopSidebarMode.AUTO))
    val mode: StateFlow<DesktopSidebarMode> = current

    fun set(value: DesktopSidebarMode) {
        DesktopPersistence().saveString(KEY, value.name)
        current.value = value
    }
}
