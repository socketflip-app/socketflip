package app.socketflip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.ceil
import kotlin.math.PI
import kotlin.math.sin

/**
 * The round floating button, drawn by hand so it can show state:
 * one colour while the tunnel is down, another while it is up, and a ring
 * that empties as the cooldown runs out.
 *
 * The same view is used for the live preview on the settings screen, where
 * [preview] pins it to a fixed state instead of following the service.
 */
class FlipButtonView(context: Context) : View(context) {

    /** A fixed state for previews; null follows the real tunnel and cooldown. */
    data class Preview(val up: Boolean, val ringFraction: Float, val pressed: Boolean = false, val alarm: Boolean = false)

    var look: Look = Look.DEFAULT
        set(value) {
            field = value
            icon.setTint(value.iconColor)
            invalidate()
        }

    /**
     * Diameter of the drawn button in px, centred; 0 fills the view. Never drawn
     * bigger than the view, so a preview cell too narrow for it shows it smaller.
     */
    var drawSize: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var preview: Preview? = null
        set(value) {
            field = value
            // A preview is a picture; its caption says what it shows.
            importantForAccessibility = if (value != null) IMPORTANT_FOR_ACCESSIBILITY_NO else IMPORTANT_FOR_ACCESSIBILITY_AUTO
            invalidate()
        }

    private var pressedUntil = 0L
    private var pulseUntil = 0L

    private val icon: Drawable = context.getDrawable(R.drawable.ic_flip)!!.mutate().apply { setTint(look.iconColor) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arc = RectF()
    private val bang = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    /** Briefly shows the "just tapped" opacity, as feedback that the tap landed. */
    fun flash() {
        pressedUntil = SystemClock.elapsedRealtime() + FLASH_MS
        invalidate()
    }

    /** Briefly swells the cooldown ring: "not yet", for a tap the cooldown refused. */
    fun pulse() {
        pulseUntil = SystemClock.elapsedRealtime() + PULSE_MS
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        val p = preview
        val up = p?.up ?: FlipVpnService.isUp
        val pressed = p?.pressed ?: (now < pressedUntil)
        val ringLeft = p?.ringFraction ?: FlipVpnService.cooldownLeft(context)
        // Emergency restart: while the cooldown runs, a tap means "restart the app".
        val alarm = p?.alarm ?: (ringLeft > 0f && Prefs.emergencyRestart(context))

        alpha = if (pressed) look.pressedAlpha else look.idleAlpha

        val fits = minOf(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom)
        val size = (if (drawSize > 0) minOf(drawSize, fits) else fits).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val stroke = size * 0.09f
        val pulsing = p == null && now < pulseUntil
        fill.color = if (alarm) ALARM_COLOR else if (up) look.upColor else look.downColor
        canvas.drawCircle(cx, cy, size / 2f - stroke / 2f, fill)

        val iconHalf = (size * 0.27f).toInt()
        icon.setBounds(cx.toInt() - iconHalf, cy.toInt() - iconHalf, cx.toInt() + iconHalf, cy.toInt() + iconHalf)
        if (alarm) {
            bang.textSize = size * 0.55f
            canvas.drawText("!", cx, cy - (bang.descent() + bang.ascent()) / 2f, bang)
        } else if (look.mirrorIcon) {
            canvas.save()
            canvas.scale(-1f, 1f, cx, cy)
            icon.draw(canvas)
            canvas.restore()
        } else {
            icon.draw(canvas)
        }

        if (ringLeft > 0f) {
            ring.color = look.ringColor
            // A pulse thickens the ring inwards and back, once.
            val swell = if (pulsing) sin(PI * (pulseUntil - now) / PULSE_MS).toFloat() else 0f
            ring.strokeWidth = stroke * (1f + swell)
            val inset = ring.strokeWidth / 2f
            arc.set(cx - size / 2f + inset, cy - size / 2f + inset, cx + size / 2f - inset, cy + size / 2f - inset)
            canvas.drawArc(arc, -90f, 360f * ringLeft, false, ring)
        }

        // Keep animating while something is changing; stop drawing once it settles.
        if (p == null && (ringLeft > 0f || pressed || pulsing)) postInvalidateOnAnimation()
    }

    /**
     * Tells TalkBack the tunnel state and the cooldown left, worked out when it asks,
     * so it is never stale. stateDescription is Android 11+; Android 10 hears it in the name.
     */
    // Taps arrive through OverlayService's touch listener, which calls this, so
    // TalkBack's double-tap and a finger tap take the same path.
    override fun performClick(): Boolean = super.performClick()

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (preview != null) return
        val state = stateWords()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) info.stateDescription = state
        else info.contentDescription = listOfNotNull(contentDescription, state).joinToString(", ")
    }

    private fun stateWords(): String {
        val tunnel = context.getString(if (FlipVpnService.isUp) R.string.a11y_tunnel_up else R.string.a11y_tunnel_down)
        val left = FlipVpnService.cooldownLeft(context)
        if (left <= 0f) return tunnel
        val s = ceil(left * Prefs.cooldownMs(context) / 1000.0).toInt()
        val words = context.resources.getQuantityString(R.plurals.a11y_cooldown_left, s, tunnel, s)
        // The red !: during the cooldown a tap offers the emergency restart.
        return if (Prefs.emergencyRestart(context)) context.getString(R.string.a11y_restart, words) else words
    }

    companion object {
        private const val FLASH_MS = 600L
        private const val PULSE_MS = 300L
        private const val ALARM_COLOR = 0xFFD32F2F.toInt()
    }
}
