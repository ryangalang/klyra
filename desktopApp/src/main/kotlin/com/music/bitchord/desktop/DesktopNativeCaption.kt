package com.music.bitchord.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.SwingUtilities

/** Color the real Windows caption without replacing its resize, Snap or system buttons. */
@Composable
internal fun DesktopNativeCaption(background: Color, foreground: Color) {
    val window = LocalDesktopWindowScope.current?.window ?: return
    DisposableEffect(window, background, foreground) {
        fun apply() {
            if (window.isDisplayable) {
                DesktopCaptionColors.apply(window, background.toArgb(), foreground.toArgb(), background.luminance() < 0.5f)
            }
        }
        val listener = HierarchyListener { event ->
            if (event.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) apply()
        }
        window.addHierarchyListener(listener)
        SwingUtilities.invokeLater { apply() }
        onDispose { window.removeHierarchyListener(listener) }
    }
}

internal interface KlyraDwmApi : StdCallLibrary {
    fun DwmSetWindowAttribute(window: HWND, attribute: Int, value: Pointer, size: Int): Int
}

internal object DesktopCaptionColors {
    internal const val DARK_MODE = 20
    internal const val CAPTION_COLOR = 35
    internal const val TEXT_COLOR = 36
    private val api by lazy { Native.load("dwmapi", KlyraDwmApi::class.java) }

    // Win32 COLORREF is 0x00BBGGRR; Compose/Java colors use 0xAARRGGBB.
    internal fun colorRef(argb: Int): Int =
        ((argb ushr 16) and 0xff) or (argb and 0xff00) or ((argb and 0xff) shl 16)

    fun apply(window: Window, background: Int, foreground: Int, dark: Boolean): Boolean {
        if (!DesktopPlatform.isWindows || !window.isDisplayable) return false
        return runCatching {
            val hwnd = HWND(Native.getWindowPointer(window))
            fun set(attribute: Int, value: Int): Int {
                val buffer = IntByReference(value)
                return api.DwmSetWindowAttribute(hwnd, attribute, buffer.pointer, 4)
            }
            // Older Windows can decline exact caption colors; dark-mode remains a best effort.
            set(DARK_MODE, if (dark) 1 else 0)
            val caption = set(CAPTION_COLOR, colorRef(background))
            val text = set(TEXT_COLOR, colorRef(foreground))
            window.background = java.awt.Color(background, true)
            caption == 0 && text == 0
        }.onFailure { DesktopTrackLog.log("Windows caption color unavailable: ${it.javaClass.simpleName}") }
            .getOrDefault(false)
    }
}
