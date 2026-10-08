package app.socketflip

import org.junit.Assert.assertEquals
import org.junit.Test

class CooldownTest {
    @Test fun settingsCooldownWhenNoAppHasItsOwn() {
        assertEquals(10_000L, Prefs.cooldownMs(10, listOf(null, null)))
    }

    @Test fun nothingTickedUsesSettings() {
        assertEquals(10_000L, Prefs.cooldownMs(10, emptyList()))
    }

    @Test fun slowestTickedAppSetsThePace() {
        assertEquals(25_000L, Prefs.cooldownMs(10, listOf(5, null, 25)))
        // An app's own shorter cooldown does not shorten it for the others.
        assertEquals(10_000L, Prefs.cooldownMs(10, listOf(5, null)))
        assertEquals(5_000L, Prefs.cooldownMs(10, listOf(5)))
    }

    @Test fun clampedToThreeToSixtySeconds() {
        assertEquals(3, Prefs.clampCooldown(0))
        assertEquals(3, Prefs.clampCooldown(-7))
        assertEquals(60, Prefs.clampCooldown(600))
        assertEquals(Prefs.COOLDOWN_DEFAULT_S, Prefs.clampCooldown(Prefs.COOLDOWN_DEFAULT_S))
    }
}
