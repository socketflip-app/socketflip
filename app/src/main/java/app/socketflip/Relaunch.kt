package app.socketflip

/**
 * Decides, four times a second after an emergency restart, whether to launch the
 * app again. Plain Kotlin with no Android calls, so it can be tested on its own.
 *
 * The restart's first launch clears the app's task. A Unity game then ends its
 * whole process a moment later (measured on a Pixel: 1.8 to 2.6 s after the task
 * was cleared), taking the new screen with it, and the home screen comes to the
 * front. The first launch after that death starts it fresh.
 *
 * Launching again while the old process is still ending CRASHES a Unity game:
 * with a launch every 250 ms the old process kept resuming new screens during its
 * own teardown and died of a native crash (null pointer in its engine, on its main
 * thread) about 6 s after the task was cleared instead of about 2 s. Seen at every
 * restart with that schedule and never with launches that waited for the old
 * process to go. So nothing is launched for [QUIET_MS] after the first launch,
 * unless Usage access already shows the home screen (or SocketFlip) in front,
 * which means the old process is gone.
 *
 * After that it launches every [EVERY_MS] whatever Usage access says, because
 * Usage access reports the app in front about two seconds late. It stops once
 * the app has been seen out of the front and then back in front. The "another
 * app is in front, the user chose it" stop is only used from [SETTLE_MS] on,
 * since the late reports would misread it before then; from then on it also
 * waits while the app is in front and was never seen gone (an app that does not
 * end its process, only brought to the front). Without Usage access it launches
 * on a fixed schedule instead ([EVERY_MS] from [QUIET_MS] to [BLIND_EVERY_UNTIL_MS],
 * then [BLIND_LATE_MS]). Either way it gives up after [CAP_MS].
 *
 * @param target the app being restarted
 * @param home the home screen app, if known
 * @param self SocketFlip's own package (its screens count like the home screen)
 * @param canSee whether Usage access is granted, so the app in front can be known
 */
class Relaunch(
    private val target: String,
    private val home: String?,
    private val self: String,
    private val canSee: Boolean,
) {
    enum class Step { LAUNCH, WAIT, DONE }

    companion object {
        /** How often the app in front is checked. */
        const val TICK_MS = 250L
        const val CAP_MS = 12_000L

        /** No launch this long after the first one, unless the old process is seen gone (it dies 1.8 to 2.6 s after it). */
        const val QUIET_MS = 2_500L

        /** Time between follow-up launches. */
        const val EVERY_MS = 500L

        /** From this long after the first launch, Usage access (about 2 s late) can be trusted to stop or wait. */
        const val SETTLE_MS = QUIET_MS + 4_000L

        /** Without Usage access: launches every [EVERY_MS] from [QUIET_MS] up to this, then at [BLIND_LATE_MS]. */
        const val BLIND_EVERY_UNTIL_MS = 6_000L
        val BLIND_LATE_MS = longArrayOf(8_000L, 10_000L)

        // The app must be seen back in front this many ticks in a row to count as restarted.
        private const val BACK_TICKS = 2
        // Another app must be in front this many ticks in a row: the user chose it, do not drag the target over it.
        private const val ELSEWHERE_TICKS = 4
    }

    /** The app has been out of the front since the restart, so its old process has ended. */
    private var gone = false
    private var back = 0
    private var elsewhere = 0
    /** When the next follow-up launch is due. */
    private var nextAt = QUIET_MS
    private var late = 0

    /** [elapsedMs] since the first launch; [front] is the app in front now, or null if unknown. */
    fun step(elapsedMs: Long, front: String?): Step {
        if (elapsedMs >= CAP_MS) return Step.DONE
        if (!canSee) return blind(elapsedMs)
        when (front) {
            null -> Unit
            target -> {
                elsewhere = 0
                if (gone) back++
            }
            home, self -> {
                // Seen gone for the first time: the old process has ended, launch now.
                if (!gone && nextAt > elapsedMs) nextAt = elapsedMs
                gone = true
                back = 0
                elsewhere = 0
            }
            else -> {
                back = 0
                elsewhere++
            }
        }
        if (gone && back >= BACK_TICKS) return Step.DONE
        if (elapsedMs >= SETTLE_MS) {
            if (front == target && !gone) return Step.WAIT
            if (front != null && front != target && front != home && front != self) {
                return if (elsewhere >= ELSEWHERE_TICKS) Step.DONE else Step.WAIT
            }
        }
        return due(elapsedMs)
    }

    /** Launch if a follow-up is due now, and set the next one [EVERY_MS] later. */
    private fun due(elapsedMs: Long): Step {
        if (elapsedMs < nextAt) return Step.WAIT
        while (nextAt <= elapsedMs) nextAt += EVERY_MS
        return Step.LAUNCH
    }

    private fun blind(elapsedMs: Long): Step {
        if (elapsedMs <= BLIND_EVERY_UNTIL_MS) return due(elapsedMs)
        val dueLate = late < BLIND_LATE_MS.size && elapsedMs >= BLIND_LATE_MS[late]
        while (late < BLIND_LATE_MS.size && elapsedMs >= BLIND_LATE_MS[late]) late++
        return when {
            dueLate -> Step.LAUNCH
            late >= BLIND_LATE_MS.size -> Step.DONE
            else -> Step.WAIT
        }
    }
}
