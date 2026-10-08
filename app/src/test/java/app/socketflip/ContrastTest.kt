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
}
