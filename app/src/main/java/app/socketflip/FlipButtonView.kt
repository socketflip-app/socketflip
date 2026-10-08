package app.socketflip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
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

    var preview: Preview? = null
        set(value) {
            field = value
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

        val size = minOf(width, height).toFloat()
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

    companion object {
        private const val FLASH_MS = 600L
        private const val PULSE_MS = 300L
        private const val ALARM_COLOR = 0xFFD32F2F.toInt()
    }
}
