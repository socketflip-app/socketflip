package app.socketflip

import app.socketflip.FrontTracker.Companion.PAUSED
import app.socketflip.FrontTracker.Companion.RESUMED
import app.socketflip.FrontTracker.Companion.STOPPED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrontTrackerTest {
    private val game = "com.example.game"
    private val home = "com.android.launcher3"
    private val other = "com.example.other"

    @Test fun nothingKnown() {
        assertNull(FrontTracker().front())
    }

    @Test fun newestResumeWins() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, home, "Home")
        t.add(1_500, PAUSED, home, "Home")
        t.add(2_000, RESUMED, game, "Main")
        assertEquals(game, t.front())
    }

    // The Pixel log: a swipe to the home screen and straight back never paused the
    // game, so its only new events were the home screen's resume and pause.
    @Test fun homePeekWithoutPausingTheTargetGivesItBack() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(10_000, RESUMED, home, "Home")
        assertEquals(home, t.front())
        t.add(11_700, PAUSED, home, "Home")
        assertEquals(game, t.front())
    }

    @Test fun repeatedEventsFromOverlappingReadsChangeNothing() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(10_000, RESUMED, home, "Home")
        t.add(11_700, PAUSED, home, "Home")
        // The next read overlaps and returns the last two again.
        t.add(10_000, RESUMED, home, "Home")
        t.add(11_700, PAUSED, home, "Home")
        assertEquals(game, t.front())
    }

    @Test fun olderEventNeverUndoesNewer() {
        val t = FrontTracker()
        t.add(5_000, PAUSED, game, "Main")
        t.add(4_000, RESUMED, game, "Main")
        assertNull(t.front())
    }

    @Test fun anyPausedOrStoppedMeansNotInFront() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(2_000, STOPPED, game, "Main")
        assertNull(t.front())
    }

    @Test fun activitiesOfOneAppAreTrackedApart() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Splash")
        t.add(2_000, RESUMED, game, "Main")
        t.add(2_100, PAUSED, game, "Splash")
        assertEquals(game, t.front())
    }

    @Test fun otherEventTypesAreIgnored() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(2_000, 5, home, null) // configuration change
        t.add(3_000, 27, home, null) // foreground service start
        assertEquals(game, t.front())
    }

    // A restarted game gets a new task and a new process but the same activity.
    @Test fun sameActivityInANewProcessIsTheSameEntry() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(3_000, PAUSED, game, "Main")
        t.add(3_100, RESUMED, home, "Home")
        assertEquals(home, t.front())
        t.add(7_400, PAUSED, home, "Home")
        t.add(7_900, RESUMED, game, "Main")
        assertEquals(game, t.front())
    }

    @Test fun resumeThenPauseOfTheSameActivityLeavesNothingInFront() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        assertEquals(game, t.front())
        t.add(2_000, PAUSED, game, "Main")
        assertNull(t.front())
    }

    // Recents and straight back: the game never pauses, so it is in front again
    // after the home screen pauses, with no new resume of its own.
    @Test fun recentsAndStraightBackKeepsTheGameWithoutANewResume() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        assertEquals(game, t.front())
        t.add(5_000, RESUMED, home, "Recents")
        assertEquals(home, t.front())
        t.add(5_800, PAUSED, home, "Recents")
        assertEquals(game, t.front())
    }

    // An overlapping read repeats an older resume after the newer pause of the same activity.
    @Test fun overlappingReadDoesNotUndoANewerPause() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(2_000, PAUSED, game, "Main")
        t.add(3_000, RESUMED, home, "Home")
        t.add(1_000, RESUMED, game, "Main")
        assertEquals(home, t.front())
    }

    @Test fun olderResumeOfAnotherAppArrivingLateDoesNotTakeTheFront() {
        val t = FrontTracker()
        t.add(5_000, RESUMED, game, "Main")
        t.add(3_000, RESUMED, home, "Home")
        assertEquals(game, t.front())
    }

    // A paused entry is kept for 60 s to order late events, then dropped.
    @Test fun pausedEntriesArePrunedAfterSixtySeconds() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, home, "Home")
        t.add(2_000, PAUSED, home, "Home")
        t.add(61_000, RESUMED, game, "Main")
        t.add(61_500, PAUSED, game, "Main")
        assertNull(t.front())
        // Inside 60 s the pause is still known, so a late older resume is ignored.
        t.add(1_500, RESUMED, home, "Home")
        assertNull(t.front())
        // Past 60 s it has been pruned, so nothing is left to order the late event against.
        t.add(62_500, RESUMED, other, "Main")
        t.add(63_000, PAUSED, other, "Main")
        assertNull(t.front())
        t.add(1_500, RESUMED, home, "Home")
        assertEquals(home, t.front())
    }

    @Test fun resumedEntriesAreNeverPruned() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(500_000, RESUMED, home, "Home")
        t.add(500_500, PAUSED, home, "Home")
        assertEquals(game, t.front())
    }

    @Test fun nullWhenNothingIsResumed() {
        val t = FrontTracker()
        t.add(1_000, RESUMED, game, "Main")
        t.add(2_000, RESUMED, home, "Home")
        t.add(3_000, PAUSED, game, "Main")
        t.add(3_000, STOPPED, home, "Home")
        assertNull(t.front())
    }
}
