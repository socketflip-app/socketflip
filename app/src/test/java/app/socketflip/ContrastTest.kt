package app.socketflip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test fun knownWcagPairs() {
        assertEquals(21.0, Look.contrast(black, white), 0.01)
        assertEquals(1.0, Look.contrast(white, white), 0.001)
        // Order does not matter.
        assertEquals(Look.contrast(black, white), Look.contrast(white, black), 0.0)
        // #777777 on white is the textbook 4.48:1.
        assertEquals(4.48, Look.contrast(0xFF777777.toInt(), white), 0.01)
    }

    @Test fun ignoresOpacityByte() {
        assertEquals(Look.contrast(black, white), Look.contrast(0x00000000, 0x00FFFFFF), 0.0)
    }

    @Test fun defaultLookIsReadable() {
        assertFalse(Look.DEFAULT.lowContrast)
    }

    @Test fun whiteIconOnYellowIsFlagged() {
        assertTrue(Look.DEFAULT.copy(downColor = 0xFFFFEB3B.toInt()).lowContrast)
    }

    @Test fun statusColoursReadableOnCardInBothModes() {
        for (night in listOf(false, true)) {
            val card = Palette.card(night)
            for (c in listOf(Palette.warning(night), Palette.good(night), Palette.bad(night), Palette.info(night))) {
                assertTrue("night=$night %08X".format(c), Look.contrast(c, card) >= Palette.MIN_TEXT_CONTRAST)
            }
        }
    }

    @Test fun previewLabelsReadable() {
        assertTrue(Look.contrast(white, Palette.PREVIEW_PANEL) >= Palette.MIN_TEXT_CONTRAST)
    }

    @Test fun paleAccentIsReplacedDarkOneKept() {
        // The old fallback blue is 3.3:1 on the light card, so it gets swapped.
        val blue = 0xFF1E88E5.toInt()
        assertTrue(Palette.readableAccent(blue, false) != blue)
        assertTrue(Look.contrast(Palette.readableAccent(blue, false), Palette.card(false)) >= Palette.MIN_TEXT_CONTRAST)
        assertTrue(Look.contrast(Palette.readableAccent(blue, true), Palette.card(true)) >= Palette.MIN_TEXT_CONTRAST)
        // An accent that already passes is left alone.
        val dark = 0xFF0B57D0.toInt()
        assertEquals(dark, Palette.readableAccent(dark, false))
    }
}
