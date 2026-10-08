package app.socketflip

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Emergency restart, for the rare time an app stays stuck on its reconnecting
 * screen after a flip and has to be restarted anyway.
 *
 * Android does not let one app kill another (from Android 14 even the old
 * "background processes" call only works on the caller itself). What SocketFlip
 * can do is relaunch the app with its task cleared: every screen it had open is
 * destroyed and it starts from the top. Many games, Unity ones in particular,
 * end their whole process when that happens, which makes it a real restart. For
 * apps that survive it, [appInfo] opens the system page with Force stop on it.
 */
object Restart {

    // One handler and one token for every follow-up, so a new restart can cancel
    // the one still running.
    private val handler = Handler(Looper.getMainLooper())
    private val TOKEN = Any()

    /** Told when a restart starts and when it has finished bringing the app back. */
    val listeners = CopyOnWriteArraySet<() -> Unit>()

    /**
     * True while a restart is still bringing the app back. The floating button
     * stays shown meanwhile, even over the home screen: from Android 15 an app may
     * only open another app from the background while one of its overlay windows is
     * visible, so a hidden button would make the follow-up launches fail silently.
     */
    @Volatile var busy = false
        private set

    private fun setBusy(on: Boolean) {
        if (busy == on) return
        busy = on
        listeners.forEach { it() }
    }

    /**
     * Relaunches [pkg] from scratch. Returns false if it has no launcher screen.
     *
     * One launch with the task cleared, then follow-ups decided by [Relaunch]: a
     * Unity game ends its whole process a moment after its task is cleared, taking
     * the new screen with it, and the first launch after that starts it fresh. For
     * apps that do not end their process, the follow-ups only bring them to the front.
     */
    fun restart(context: Context, pkg: String): Boolean {
        // A second restart replaces the first one's follow-ups instead of adding to them.
        handler.removeCallbacksAndMessages(TOKEN)
        // A fresh start should not come up inside SocketFlip's tunnel.
        if (FlipVpnService.isUp) FlipVpnService.stop(context)
        val clear = context.packageManager.getLaunchIntentForPackage(pkg)
        if (clear == null) {
            setBusy(false)
            return false
        }
        clear.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (!launch(context, pkg, clear)) {
            setBusy(false)
            return false
        }
        Prefs.countRestart(context)
        val see = ForegroundWatcher.granted(context)
        // An hour back, so the app's own earlier resume is in the picture.
        val front = if (see) UsageFront(context, 60 * 60 * 1000L) else null
        val plan = Relaunch(pkg, homePackage(context), context.packageName, see)
        val start = SystemClock.uptimeMillis()
        setBusy(true)
        val tick = object : Runnable {
            override fun run() {
                when (plan.step(SystemClock.uptimeMillis() - start, front?.poll())) {
                    Relaunch.Step.DONE -> {
                        setBusy(false)
                        return
                    }
                    Relaunch.Step.LAUNCH -> context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        launch(context, pkg, it, quiet = true)
                    }
                    Relaunch.Step.WAIT -> Unit
                }
                handler.postAtTime(this, TOKEN, SystemClock.uptimeMillis() + Relaunch.TICK_MS)
            }
        }
        handler.postAtTime(tick, TOKEN, start + Relaunch.TICK_MS)
        return true
    }

    /** The home screen app (visible to SocketFlip through the HOME entry in the manifest's queries). */
    private fun homePackage(context: Context): String? = context.packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
    )?.activityInfo?.packageName

    private fun launch(context: Context, pkg: String, intent: Intent, quiet: Boolean = false): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        // Background start refused, or the app vanished: say so (once) rather than do nothing.
        if (!quiet) Toast.makeText(context, context.getString(R.string.restart_failed, Prefs.label(context, pkg)),
            Toast.LENGTH_LONG).show()
        false
    }

    /** The system App info page for [pkg], which has the Force stop button. */
    fun appInfo(context: Context, pkg: String) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            // Nothing to open.
        }
    }
}
