package app.socketflip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.hypot

/** Keeps a small draggable button on screen; a tap flips the tunnel once. */
class OverlayService : Service() {

    companion object {
        const val ACTION_STOP = "app.socketflip.action.STOP_OVERLAY"

        private const val CHANNEL = "overlay"
        private const val NOTIFICATION_ID = 1
        private const val SIZE_DP = 52
        private const val IDLE_COLOR = 0xCC1E88E5.toInt()
        private const val FLASH_COLOR = 0xEEFFA000.toInt()
        private const val FLASH_MS = 600L

        @Volatile var running = false
            private set
    }

    private lateinit var wm: WindowManager
    private var button: ImageView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(WindowManager::class.java)
        startInForeground()
        addButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
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
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val flip = PendingIntent.getService(
            this, 1, Intent(this, FlipVpnService::class.java).setAction(FlipVpnService.ACTION_FLIP), flags
        )
        val stop = PendingIntent.getService(
            this, 2, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), flags
        )
        val open = PendingIntent.getActivity(this, 3, Intent(this, MainActivity::class.java), flags)
        val target = Prefs.target(this)?.let { Prefs.label(this, it) } ?: ""
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_flip)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_text, target))
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.action_flip), flip).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.action_stop), stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun addButton() {
        val size = (SIZE_DP * resources.displayMetrics.density).toInt()
        val pad = size / 4
        val background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(IDLE_COLOR)
        }
        val view = ImageView(this).apply {
            setImageResource(R.drawable.ic_flip)
            setPadding(pad, pad, pad, pad)
            this.background = background
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
        view.setOnClickListener { tapped(it, background) }
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
                    if (dragging) Prefs.setPosition(this, lp.x, lp.y) else v.performClick()
                    true
                }
                else -> false
            }
        }
        wm.addView(view, lp)
        button = view
    }

    private fun tapped(view: View, background: GradientDrawable) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        background.setColor(FLASH_COLOR)
        view.postDelayed({ background.setColor(IDLE_COLOR) }, FLASH_MS)
        FlipVpnService.flip(this)
    }
}
