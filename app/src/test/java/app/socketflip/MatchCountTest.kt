package app.socketflip

import org.junit.Assert.assertEquals
import org.junit.Test

class MatchCountTest {
    private val gap = 6 * 60_000L

    @Test fun nothingCountedYet() {
        assertEquals(0, MatchCount.current(5, 0L, 1_000_000L, gap))
    }

    @Test fun countStandsInsideTheGap() {
        assertEquals(7, MatchCount.current(7, 1_000_000L, 1_000_000L + gap, gap))
    }

    @Test fun quietSpellStartsANewGame() {
        assertEquals(0, MatchCount.current(7, 1_000_000L, 1_000_001L + gap, gap))
    }

    @Test fun resetInCountsDownAndStopsAtZero() {
        assertEquals(gap - 1000L, MatchCount.resetIn(1_000_000L, 1_001_000L, gap))
        assertEquals(0L, MatchCount.resetIn(1_000_000L, 1_000_000L + gap + 5, gap))
    }

    @Test fun badgeShadeFollowsTheLimit() {
        assertEquals(0, MatchCount.level(8, 10))
        assertEquals(1, MatchCount.level(9, 10))
        assertEquals(2, MatchCount.level(10, 10))
        assertEquals(2, MatchCount.level(12, 10))
        // A limit of 1: the first flip is already at it, nothing is ever amber before.
        assertEquals(2, MatchCount.level(1, 1))
    }
}
