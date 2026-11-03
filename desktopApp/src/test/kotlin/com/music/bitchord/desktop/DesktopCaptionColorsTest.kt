package com.music.bitchord.desktop

import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopCaptionColorsTest {
    @Test fun convertsArgbToWin32ColorRef() {
        assertEquals(0x00563412, DesktopCaptionColors.colorRef(0xff123456.toInt()))
    }

    @Test fun nativeCaptionTracksDarkAndLightWithoutRemovingDecoration() {
        if (!DesktopPlatform.isWindows || System.getenv("KLYRA_WINDOW_TEST") != "1") return
        SwingUtilities.invokeAndWait {
            val frame = JFrame("Klyra caption verification")
            try {
                frame.setSize(400, 240)
                frame.addNotify()
                for ((background, foreground) in listOf(0xff101012.toInt() to 0xfff4f4f4.toInt(), 0xfffafafa.toInt() to 0xff181818.toInt())) {
                    assertTrue(DesktopCaptionColors.apply(frame, background, foreground, background == 0xff101012.toInt()))
                    // Caption/text colors are set-only DWM attributes. Verify the native setter
                    // accepts both palettes; actual rendering is a separate visual smoke check.
                    assertEquals(background, frame.background.rgb)
                    assertFalse(frame.isUndecorated)
                    assertTrue(frame.isResizable)
                }
            } finally { frame.dispose() }
        }
    }
}
