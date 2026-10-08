package app.socketflip

import app.socketflip.Relaunch.Step.DONE
import app.socketflip.Relaunch.Step.LAUNCH
import app.socketflip.Relaunch.Step.WAIT
import org.junit.Assert.assertEquals
import org.junit.Test

class RelaunchTest {
    private val game = "com.example.game"
    private val home = "com.android.launcher3"
    private val self = "app.socketflip"
    private val other = "com.example.chat"

    private fun seeing() = Relaunch(game, home, self, canSee = true)
    private fun blind() = Relaunch(game, home, self, canSee = false)

    /** Runs [fronts] (one per tick, from 250 ms) and returns the steps. */
    private fun run(r: Relaunch, vararg fronts: String?) =
        fronts.mapIndexed { i, f -> r.step((i + 1) * Relaunch.TICK_MS, f) }

    /** Feeds [front] on every tick from 250 ms up to and including [untilMs]; returns the times it launched. */
    private fun launches(r: Relaunch, untilMs: Long, front: String?) =
        (1..(untilMs / Relaunch.TICK_MS)).map { it * Relaunch.TICK_MS }.filter { r.step(it, front) == LAUNCH }

    private val every500From2500To6000 = (2_500L..6_000L step 500L).toList()

    @Test fun checksFourTimesASecondLaunchesTwiceASecondAfterAQuietWindow() {
        assertEquals(250L, Relaunch.TICK_MS)
        assertEquals(2_500L, Relaunch.QUIET_MS)
        assertEquals(500L, Relaunch.EVERY_MS)
        assertEquals(6_500L, Relaunch.SETTLE_MS)
    }

    // Launching every 250 ms into the dying old process crashed a Unity game at every restart.
    // The old process dies 1.8 to 2.6 s after the task is cleared: nothing before 2.5 s.
    @Test fun noLaunchInTheQuietWindowWhileTheGameStillLooksInFront() {
        val r = seeing()
        assertEquals(List(9) { WAIT }, (1..9).map { r.step(it * Relaunch.TICK_MS, game) })
        assertEquals(listOf(LAUNCH, WAIT, LAUNCH, WAIT), (10..13).map { r.step(it * Relaunch.TICK_MS, game) })
    }

    // Usage access reports about 2 s late: after the quiet window it launches whatever it says.
    @Test fun afterTheQuietWindowLaunchesEveryHalfSecondEvenWhileTheGameLooksInFront() {
        assertEquals(every500From2500To6000, launches(seeing(), 6_250, game))
    }

    @Test fun anotherAppReportedEarlyNeitherLaunchesInTheQuietWindowNorStopsIt() {
        val r = seeing()
        assertEquals(every500From2500To6000, launches(r, 6_250, other))
        assertEquals(DONE, r.step(6_500, other))
    }

    @Test fun homeSeenInTheQuietWindowLaunchesAtOnce() {
        val steps = run(seeing(), game, game, home, home, home, game, game)
        assertEquals(listOf(WAIT, WAIT, LAUNCH, WAIT, LAUNCH, WAIT, DONE), steps)
    }

    @Test fun socketFlipsOwnScreenCountsAsGone() {
        assertEquals(listOf(WAIT, LAUNCH, WAIT, DONE), run(seeing(), game, self, game, game))
    }

    @Test fun backForOneTickIsNotEnough() {
        assertEquals(listOf(LAUNCH, WAIT, LAUNCH, WAIT, DONE), run(seeing(), home, game, home, game, game))
    }

    @Test fun theHomeScreenNeverStopsItAndLaunchesStayHalfASecondApart() {
        val r = seeing()
        assertEquals((250L..11_750L step 500L).toList(), launches(r, Relaunch.CAP_MS - Relaunch.TICK_MS, home))
        assertEquals(DONE, r.step(Relaunch.CAP_MS, home))
    }

    @Test fun afterSettlingAnAppThatNeverEndsItsProcessIsLeftAlone() {
        val r = seeing()
        assertEquals(every500From2500To6000, launches(r, 6_250, game))
        assertEquals(List(21) { WAIT }, (26..46).map { r.step(it * Relaunch.TICK_MS, game) })
        assertEquals(DONE, r.step(Relaunch.CAP_MS, game))
    }

    @Test fun afterSettlingTheHomeScreenStillLaunches() {
        val r = seeing()
        launches(r, 6_250, game)
        assertEquals(LAUNCH, r.step(6_500, home))
        assertEquals(WAIT, r.step(6_750, game))
        assertEquals(DONE, r.step(7_000, game))
    }

    @Test fun afterSettlingAnotherAppForASecondStopsIt() {
        val r = seeing()
        launches(r, 6_250, game)
        assertEquals(listOf(WAIT, WAIT, WAIT, DONE), (26..29).map { r.step(it * Relaunch.TICK_MS, other) })
    }

    @Test fun afterSettlingAnotherAppBrieflyDoesNot() {
        val r = seeing()
        launches(r, 6_250, game)
        assertEquals(listOf(WAIT, LAUNCH, WAIT, LAUNCH), listOf(other, home, other, home)
            .mapIndexed { i, f -> r.step((26 + i) * Relaunch.TICK_MS, f) })
    }

    @Test fun unknownFrontWaitsOutTheQuietWindowThenLaunchesEveryHalfSecond() {
        val r = seeing()
        assertEquals((2_500L..11_750L step 500L).toList(), launches(r, Relaunch.CAP_MS - Relaunch.TICK_MS, null))
        assertEquals(DONE, r.step(Relaunch.CAP_MS, null))
    }

    @Test fun blindWaitsTwoAndAHalfSecondsThenEveryHalfSecondToSixThenEightAndTen() {
        assertEquals(every500From2500To6000 + listOf(8_000L, 10_000L), launches(blind(), 10_000, null))
    }

    @Test fun blindIgnoresWhatItIsToldAboutTheFront() {
        val r = blind()
        assertEquals(List(9) { WAIT } + LAUNCH, (1..10).map { r.step(it * Relaunch.TICK_MS, home) })
    }

    @Test fun blindIsDoneAfterTheLastSlot() {
        val r = blind()
        launches(r, 10_000, null)
        assertEquals(DONE, r.step(10_250, null))
    }

    @Test fun lateTicksDoNotBurstLaunches() {
        val r = seeing()
        // The phone was busy and the first tick after the quiet window came at 3.7 s: one launch, not three.
        assertEquals(LAUNCH, r.step(3_700, game))
        assertEquals(WAIT, r.step(3_950, game))
        assertEquals(LAUNCH, r.step(4_200, game))
    }
}
