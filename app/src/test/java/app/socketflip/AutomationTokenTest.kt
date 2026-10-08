package app.socketflip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationTokenTest {
    @Test fun onlyTheExactTokenIsAccepted() {
        val token = Prefs.randomCode()
        assertTrue(Prefs.tokenMatches(token, token))
        assertFalse(Prefs.tokenMatches(null, token))
        assertFalse(Prefs.tokenMatches("", token))
        assertFalse(Prefs.tokenMatches(token.uppercase(), token))
        assertFalse(Prefs.tokenMatches(token.dropLast(1), token))
        assertFalse(Prefs.tokenMatches(token + "x", token))
    }

    @Test fun codesAreSixteenEasyToReadCharacters() {
        repeat(50) {
            val code = Prefs.randomCode()
            assertEquals(16, code.length)
            // No l, o, 0 or 1: it may be typed in by hand.
            assertTrue(code, code.all { it in "abcdefghijkmnpqrstuvwxyz23456789" })
        }
    }

    @Test fun codesDiffer() {
        assertNotEquals(Prefs.randomCode(), Prefs.randomCode())
    }
}
