package app.socketflip

/**
 * Works out which app is in front from Usage access activity events. Plain
 * Kotlin with no Android calls, so it can be tested on its own.
 *
 * "The app of the newest ACTIVITY_RESUMED" is not enough: a game can stay
 * resumed while the home screen shows for a moment (a swipe into recents and
 * straight back), and when the home screen then pauses the game gets no new
 * resume event at all. So this keeps, per activity, whether its newest event was
 * a resume or a pause/stop, and the app in front is the one with the newest
 * resume that has not been paused since.
 */
class FrontTracker {

    companion object {
        // UsageEvents.Event values, copied so this class needs no Android classes.
        const val RESUMED = 1
        const val PAUSED = 2
        const val STOPPED = 23
        const val DESTROYED = 24

        // Paused activities are only kept to put late or repeated events in order.
        private const val KEEP_PAUSED_MS = 60_000L
    }

    private class State(val pkg: String, val resumed: Boolean, val time: Long)

    private val states = HashMap<String, State>()
    private var newest = 0L

    /** Feeds one event. Events may repeat (overlapping queries); older ones never undo newer ones. */
    fun add(time: Long, type: Int, pkg: String, activity: String?) {
        val resumed = when (type) {
            RESUMED -> true
            PAUSED, STOPPED, DESTROYED -> false
            else -> return
        }
        val key = pkg + "/" + activity.orEmpty()
        val old = states[key]
        if (old != null && time < old.time) return
        states[key] = State(pkg, resumed, time)
        if (time > newest) newest = time
    }

    /** The app in front, or null when no activity is resumed (screen off, lock screen) or nothing is known yet. */
    fun front(): String? {
        states.values.removeAll { !it.resumed && it.time < newest - KEEP_PAUSED_MS }
        return states.values.filter { it.resumed }.maxByOrNull { it.time }?.pkg
    }
}
