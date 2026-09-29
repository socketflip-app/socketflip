package app.socketflip

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process

/**
 * Follows which app is on screen, using Usage Access (Settings > Apps > Special
 * app access > Usage access). Only used for the two optional settings that need
 * it: showing the button over the target app only, and dropping the tunnel when
 * the target app is left. Polls about once a second, and only while one of them
 * is switched on.
 */
class ForegroundWatcher(private val context: Context, private val changed: (String) -> Unit) {

    companion object {
        private const val POLL_MS = 1000L
        // How far back the first read looks: a game can sit in front for a long time.
        private const val LOOKBACK_MS = 60 * 60 * 1000L

        fun granted(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java)
            return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
                AppOpsManager.MODE_ALLOWED
        }
    }

    /** The app in front, or null before the first answer. */
    var current: String? = null
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var front = UsageFront(context, LOOKBACK_MS)
    private var running = false

    private val poll = object : Runnable {
        override fun run() {
            check()
            if (running) handler.postDelayed(this, POLL_MS)
        }
    }

    fun start() {
        if (running) return
        running = true
        front = UsageFront(context, LOOKBACK_MS)
        handler.post(poll)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(poll)
    }

    private fun check() {
        val latest = front.poll()
        if (latest != null && latest != current) {
            current = latest
            changed(latest)
        }
    }
}

/**
 * Reads Usage access events a slice at a time into a [FrontTracker]. The first
 * read looks back [lookbackMs] so an app already open is known straight away;
 * later reads overlap the previous one a little, because events can be logged a
 * moment late (the tracker ignores the repeats).
 */
class UsageFront(context: Context, lookbackMs: Long) {
    private val usage = context.getSystemService(UsageStatsManager::class.java)
    private val tracker = FrontTracker()
    private var since = System.currentTimeMillis() - lookbackMs

    /** The app in front now, or null if unknown (no Usage access, or nothing resumed). */
    fun poll(): String? {
        val now = System.currentTimeMillis()
        try {
            val events = usage.queryEvents(since, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                tracker.add(e.timeStamp, e.eventType, e.packageName, e.className)
            }
        } catch (e: SecurityException) {
            // Usage Access was taken away; behave as if nothing is known.
            return null
        }
        since = now - OVERLAP_MS
        return tracker.front()
    }

    private companion object {
        const val OVERLAP_MS = 2000L
    }
}
