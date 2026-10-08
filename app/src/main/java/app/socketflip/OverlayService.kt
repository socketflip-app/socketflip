package app.socketflip

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.hypot

/** Keeps a small draggable button on screen; a tap flips the tunnel once. */
class OverlayService : Service() {

    companion object {
        const val ACTION_STOP = "app.socketflip.action.STOP_OVERLAY"

        private const val CHANNEL = "overlay"
        private const val NOTIFICATION_ID = 1
        // How long another app must stay in front before it counts as leaving the target.
        private const val LEAVE_CONFIRM_MS = 2000L

        @Volatile var running = false
            private set
    }

    private lateinit var wm: WindowManager
    private var button: FlipButtonView? = null
    private var params: WindowManager.LayoutParams? = null
    private val redraw: () -> Unit = { button?.invalidate() }
    // An emergency restart keeps the button shown until the app is back (see Restart.busy).
    private val restartChanged: () -> Unit = { setShown(shouldShow()) }

    // Held in a field: SharedPreferences only keeps a weak reference to listeners.
    private val settingsChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyLook()
        updateWatcher()
        updateNotification()
    }
    private var notifiedTarget: String? = null

    private var watcher: ForegroundWatcher? = null
    private var foreground: String? = null
    // The last app in front, other than SocketFlip itself, was a target.
    private var inTarget = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(WindowManager::class.java)
        startInForeground()
        addButton()
        FlipVpnService.listeners.add(redraw)
        Restart.listeners.add(restartChanged)
        Prefs.listen(this, settingsChanged)
        updateWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        FlipVpnService.listeners.remove(redraw)
        Restart.listeners.remove(restartChanged)
        Prefs.unlisten(this, settingsChanged)
        watcher?.stop()
        watcher = null
        handler.removeCallbacksAndMessages(null)
        // Never crash over the target app: the view may already be detached.
        button?.let {
            try {
                wm.removeView(it)
            } catch (e: IllegalArgumentException) {
                // Not attached; nothing to remove.
            }
        }
        button = null
        // Leave the phone as we found it.
        if (FlipVpnService.isUp) FlipVpnService.stop(this)
        super.onDestroy()
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Keeps the notification naming the active target when it changes. */
    private fun updateNotification() {
        if (Prefs.checkedLabels(this) == notifiedTarget) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val flip = PendingIntent.getService(
            this, 1, Intent(this, FlipVpnService::class.java).setAction(FlipVpnService.ACTION_FLIP), flags
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), flags
        )
        val open = PendingIntent.getActivity(this, 3, Intent(this, MainActivity::class.java), flags)
        notifiedTarget = Prefs.checkedLabels(this)
        val target = notifiedTarget.orEmpty()
        // With every app unticked the sentence would end mid-way; say what to do instead.
        val text = if (target.isEmpty()) getString(R.string.notif_text_none) else getString(R.string.notif_text, target)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_flip)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.action_flip), flip).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.action_stop), stop).build())
            .build()
    }

    private fun addButton() {
        val look = Prefs.look(this)
        val size = px(look.sizeDp)
        val view = FlipButtonView(this).apply {
            this.look = look
            contentDescription = getString(R.string.action_flip)
        }
        val (x, y) = Prefs.position(this)
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        // Press and hold is for moving the button: a hold released without moving
        // is not a tap, so it never flips and never offers the emergency restart.
        val holdMs = ViewConfiguration.getLongPressTimeout().toLong()
        view.setOnClickListener { tapped(view) }
        view.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = lp.x; startY = lp.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) dragging = true
                    if (dragging) {
                        lp.x = startX + dx.toInt()
                        lp.y = startY + dy.toInt()
                        wm.updateViewLayout(v, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    when {
                        dragging -> if (Prefs.snapToEdge(this)) snap(v, lp) else Prefs.setPosition(this, lp.x, lp.y)
                        e.eventTime - e.downTime < holdMs -> v.performClick()
                        else -> Unit
                    }
                    true
                }
                else -> false
            }
        }
        keepOnScreen(lp)
        try {
            wm.addView(view, lp)
        } catch (e: RuntimeException) {
            // The system refused the overlay (permission just revoked, a managed phone,
            // an OEM pop-up switch). Explain instead of crashing.
            Toast.makeText(this, R.string.need_overlay, Toast.LENGTH_LONG).show()
            running = false
            stopSelf()
            return
        }
        button = view
        params = lp
    }

    /**
     * Keeps the whole button on screen. The saved position is wherever it was last
     * dragged, so a button dragged low in portrait would sit below the bottom of the
     * screen in a landscape game, running but invisible. Also re-checked whenever the
     * screen rotates.
     */
    private fun keepOnScreen(lp: WindowManager.LayoutParams) {
        val screen = resources.displayMetrics
        lp.x = lp.x.coerceIn(0, maxOf(0, screen.widthPixels - lp.width))
        lp.y = lp.y.coerceIn(0, maxOf(0, screen.heightPixels - lp.height))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val view = button ?: return
        val lp = params ?: return
        keepOnScreen(lp)
        try {
            wm.updateViewLayout(view, lp)
        } catch (e: IllegalArgumentException) {
            // Not attached any more.
        }
    }

    /** Runs the foreground watcher only while a setting needs it and Usage Access is granted. */
    private fun updateWatcher() {
        val needed = (Prefs.onlyOverTarget(this) || Prefs.dropOnLeave(this)) && ForegroundWatcher.granted(this)
        if (needed && watcher == null) {
            watcher = ForegroundWatcher(this) { foregroundChanged(it) }.also { it.start() }
        } else if (!needed && watcher != null) {
            watcher?.stop()
            watcher = null
            foreground = null
        }
        setShown(shouldShow())
    }

    private fun foregroundChanged(pkg: String) {
        foreground = pkg
        setShown(shouldShow())
        // SocketFlip's own screens and dialogs are not leaving the target.
        if (pkg == packageName) return
        if (isTarget(pkg)) {
            inTarget = true
            handler.removeCallbacks(dropIfLeft)
            return
        }
        if (!inTarget) return
        inTarget = false
        // Play's purchase sheet, a sign-in page or the other half of a split screen
        // come from other apps but sit on top of the target without leaving it. So
        // only the home screen counts at once; any other app must stay in front a
        // moment. dropIfLeft also waits out the cooldown.
        handler.removeCallbacks(dropIfLeft)
        handler.postDelayed(dropIfLeft, if (pkg == homePackage) 0L else LEAVE_CONFIRM_MS)
    }

    private fun isTarget(pkg: String?) = pkg != null && pkg in Prefs.checked(this)

    /** The home screen app, which is always a real "left the target". */
    private val homePackage: String? by lazy {
        packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName
    }

    /**
     * Drops the tunnel once the target has really been left. The target is in the
     * background then, so this disconnect costs nothing, and the VPN key does not
     * linger in the status bar. Inside the cooldown the target may still be
     * reconnecting from the last tap, and a second disconnect then can wedge it, so
     * the drop waits for the cooldown to end (and is cancelled if the target comes back).
     */
    private val dropIfLeft: Runnable = object : Runnable {
        override fun run() {
            if (isTarget(foreground) || !Prefs.dropOnLeave(this@OverlayService) || !FlipVpnService.isUp) return
            val wait = (FlipVpnService.cooldownLeft(this@OverlayService) * Prefs.cooldownMs(this@OverlayService)).toLong()
            if (wait > 0) {
                handler.postDelayed(this, wait + 100)
                return
            }
            FlipVpnService.stop(this@OverlayService)
        }
    }

    private fun shouldShow(): Boolean {
        if (watcher == null || !Prefs.onlyOverTarget(this) || Restart.busy) return true
        val fg = foreground ?: return true
        return fg in Prefs.checked(this) || fg == packageName
    }

    /**
     * Hides or shows the button. A hidden button's window must also stop taking
     * touches, or it would swallow taps meant for the app underneath.
     */
    private fun setShown(shown: Boolean) {
        val view = button ?: return
        val lp = params ?: return
        val visibility = if (shown) View.VISIBLE else View.GONE
        if (view.visibility == visibility) return
        view.visibility = visibility
        lp.flags = if (shown) lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        else lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        try {
            wm.updateViewLayout(view, lp)
        } catch (e: IllegalArgumentException) {
            // Not attached any more.
        }
    }

    /** Redraws the button after a change on the settings screen, resizing it if needed. */
    private fun applyLook() {
        val view = button ?: return
        val lp = params ?: return
        val look = Prefs.look(this)
        view.look = look
        val size = px(look.sizeDp)
        if (lp.width != size) {
            lp.width = size
            lp.height = size
            keepOnScreen(lp)
            try {
                wm.updateViewLayout(view, lp)
            } catch (e: IllegalArgumentException) {
                // Not attached any more; nothing to resize.
            }
        }
    }

    /** Slides the button to whichever side of the screen is nearer, then saves the spot. */
    private fun snap(view: View, lp: WindowManager.LayoutParams) {
        val screen = resources.displayMetrics.widthPixels
        val target = if (lp.x + lp.width / 2 < screen / 2) 0 else screen - lp.width
        ValueAnimator.ofInt(lp.x, target).apply {
            duration = 180
            addUpdateListener {
                lp.x = it.animatedValue as Int
                try {
                    wm.updateViewLayout(view, lp)
                } catch (e: IllegalArgumentException) {
                    cancel()
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = Prefs.setPosition(this@OverlayService, lp.x, lp.y)
            })
            start()
        }
    }

    private fun px(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun tapped(view: FlipButtonView) {
        val cooling = FlipVpnService.cooldownLeft(this) > 0f
        // Emergency restart: while the cooldown runs (the red !), a tap offers to
        // restart the app, always behind the confirmation dialog.
        val restart = cooling && Prefs.emergencyRestart(this)
        if (cooling && !restart) {
            // Refused: the flip service would drop it anyway. Say so by feel and sight,
            // never with the success buzz and flash, since the hint toast may be off.
            // REJECT is Android 11+; Android 10 gets no buzz, only the ring pulse.
            if (Prefs.haptics(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            }
            view.pulse()
            if (Prefs.hints(this)) Toast.makeText(this, R.string.cooldown, Toast.LENGTH_SHORT).show()
            return
        }
        // CONFIRM is Android 11+; Android 10 gets the plain tap feedback.
        if (Prefs.haptics(this)) view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY
        )
        if (restart) {
            confirmRestart()
            return
        }
        view.flash()
        FlipVpnService.flip(this)
    }

    /**
     * Asks before restarting: a stray tap during the cooldown must never restart a
     * game by itself. Drawn as an overlay dialog so the app underneath stays put.
     */
    private fun confirmRestart() {
        val targets = Prefs.checked(this).filter { packageManager.getLaunchIntentForPackage(it) != null }
        if (targets.isEmpty()) {
            Toast.makeText(this, R.string.target_unset, Toast.LENGTH_SHORT).show()
            return
        }
        // The app on screen, if Usage access says which; the only ticked app; or ask.
        val choice = foreground?.takeIf { it in targets } ?: targets.singleOrNull()
        try {
            RestartDialog.show(this, targets, choice)
        } catch (e: Exception) {
            // Never crash over the target app, and never restart without asking: a
            // restart can throw away a match in progress.
            Toast.makeText(this, R.string.restart_cannot_ask, Toast.LENGTH_LONG).show()
        }
    }
}
