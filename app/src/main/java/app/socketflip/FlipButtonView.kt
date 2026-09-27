package app.socketflip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View

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
    data class Preview(val up: Boolean, val ringFraction: Float, val pressed: Boolean = false)

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

    private val icon: Drawable = context.getDrawable(R.drawable.ic_flip)!!.mutate().apply { setTint(look.iconColor) }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arc = RectF()

    /** Briefly shows the "just tapped" opacity, as feedback that the tap landed. */
    fun flash() {
        pressedUntil = SystemClock.elapsedRealtime() + FLASH_MS
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        val p = preview
        val up = p?.up ?: FlipVpnService.isUp
        val pressed = p?.pressed ?: (now < pressedUntil)
        val ringLeft = p?.ringFraction ?: FlipVpnService.cooldownLeft(context)

        alpha = if (pressed) look.pressedAlpha else look.idleAlpha

        val size = minOf(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val stroke = size * 0.09f
        fill.color = if (up) look.upColor else look.downColor
        canvas.drawCircle(cx, cy, size / 2f - stroke / 2f, fill)

        val iconHalf = (size * 0.27f).toInt()
        icon.setBounds(cx.toInt() - iconHalf, cy.toInt() - iconHalf, cx.toInt() + iconHalf, cy.toInt() + iconHalf)
        icon.draw(canvas)

        if (ringLeft > 0f) {
            ring.color = look.ringColor
            ring.strokeWidth = stroke
            val inset = stroke / 2f
            arc.set(cx - size / 2f + inset, cy - size / 2f + inset, cx + size / 2f - inset, cy + size / 2f - inset)
            canvas.drawArc(arc, -90f, 360f * ringLeft, false, ring)
        }

        // Keep animating while something is changing; stop drawing once it settles.
        if (p == null && (ringLeft > 0f || pressed)) postInvalidateOnAnimation()
    }

    companion object {
        private const val FLASH_MS = 600L
    }
}
