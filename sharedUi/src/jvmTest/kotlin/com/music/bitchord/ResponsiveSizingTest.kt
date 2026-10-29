package com.music.bitchord

import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.components.LIBRARY_GRID_SPACING
import com.music.bitchord.ui.components.heroCardWidth
import com.music.bitchord.ui.components.libraryGrid
import com.music.bitchord.ui.components.trackColumnWidth
import com.music.bitchord.ui.player.landscapePlayerAvailable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sizing invariants, not a substitute for visual/device testing. */
class ResponsiveSizingTest {
    @Test fun phoneAndTabletGridsFitTheirAvailableContentWidth() {
        // Narrow split windows, phones, tablets and tablet landscape (in dp).
        for (window in listOf(280, 320, 360, 393, 432, 600, 800, 1024, 1280)) {
            val available = (window - 40).dp
            val grid = libraryGrid(available)
            assertTrue("Invalid card size at $window dp", grid.cardWidth.value > 0f)
            val occupied = grid.cardWidth * grid.columns + LIBRARY_GRID_SPACING * (grid.columns - 1)
            assertEquals("Grid overflows at $window dp", available.value, occupied.value, 0.1f)
        }
    }

    @Test fun shelvesRemainWithinTheWindowWithoutStretchingAcrossTablets() {
        for (available in listOf(240, 280, 320, 600, 800, 1240)) {
            assertTrue(heroCardWidth(available.dp) < available.dp)
            assertTrue(trackColumnWidth(available.dp) < available.dp)
        }
        assertEquals(heroCardWidth(800.dp), heroCardWidth(1240.dp))
        assertEquals(trackColumnWidth(800.dp), trackColumnWidth(1240.dp))
    }

    @Test fun portraitTabletsKeepThePortraitPlayerAndRotationUsesAvailableSpace() {
        assertFalse(landscapePlayerAvailable(393.dp, 852.dp))
        assertFalse(landscapePlayerAvailable(800.dp, 1280.dp))
        assertTrue(landscapePlayerAvailable(852.dp, 393.dp))
        assertTrue(landscapePlayerAvailable(1280.dp, 800.dp))
        // Returning to a narrow split window must not retain a stale tablet layout.
        assertFalse(landscapePlayerAvailable(360.dp, 800.dp))
    }
}
