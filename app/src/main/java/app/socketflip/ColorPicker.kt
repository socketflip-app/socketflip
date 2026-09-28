package app.socketflip

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Hue and saturation wheel: angle is hue, distance from the centre is saturation.
 * Brightness is a separate slider, and the wheel darkens to match it.
 */
class ColorWheelView(context: Context) : View(context) {

    /** Hue 0..360, saturation 0..1, value 0..1. */
    val hsv = floatArrayOf(0f, 0f, 1f)
    var onChange: (() -> Unit)? = null

    private val wheel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3 * resources.displayMetrics.density
    }
    private var radius = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2f - marker.strokeWidth * 3
        val cx = w / 2f
        val cy = h / 2f
        val hues = IntArray(7) { Color.HSVToColor(floatArrayOf(it * 60f % 360f, 1f, 1f)) }
        // SweepGradient runs clockwise from 3 o'clock, the same direction as angle() below.
        val sweep = SweepGradient(cx, cy, hues, null)
        val fade = RadialGradient(cx, cy, radius, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP)
        wheel.shader = ComposeShader(sweep, fade, PorterDuff.Mode.SRC_OVER)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val side = min(w, (260 * resources.displayMetrics.density).toInt())
        setMeasuredDimension(w, side)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, radius, wheel)
        shade.color = Color.argb(((1f - hsv[2]) * 255).toInt(), 0, 0, 0)
        canvas.drawCircle(cx, cy, radius, shade)

        val a = Math.toRadians(hsv[0].toDouble())
        val mx = cx + (cos(a) * hsv[1] * radius).toFloat()
        val my = cy + (sin(a) * hsv[1] * radius).toFloat()
        val r = marker.strokeWidth * 3
        marker.color = Color.BLACK
        canvas.drawCircle(mx, my, r + marker.strokeWidth, marker)
        marker.color = Color.WHITE
        canvas.drawCircle(mx, my, r, marker)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Stop the scrolling settings page from stealing a drag across the wheel.
                parent?.requestDisallowInterceptTouchEvent(true)
                val dx = e.x - width / 2f
                val dy = e.y - height / 2f
                var angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                if (angle < 0) angle += 360f
                hsv[0] = angle
                hsv[1] = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                invalidate()
                onChange?.invoke()
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}

/** Colour wheel, brightness slider and hex box in a dialog. Colours are always opaque. */
object ColorPicker {

    fun show(context: Context, title: String, initial: Int, picked: (Int) -> Unit) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val wheel = ColorWheelView(context)
        Color.colorToHSV(initial, wheel.hsv)
        val swatch = View(context)
        val swatchBg = GradientDrawable().apply { cornerRadius = dp(8).toFloat() }
        swatch.background = swatchBg
        val brightness = SeekBar(context).apply { max = 100 }
        val hex = EditText(context).apply {
            filters = arrayOf(InputFilter.LengthFilter(7), InputFilter.AllCaps())
            isSingleLine = true
        }

        fun current() = Color.HSVToColor(wheel.hsv)
        var updatingHex = false
        fun refresh(fromHex: Boolean = false) {
            val c = current()
            swatchBg.setColor(c)
            if (!fromHex) {
                updatingHex = true
                hex.setText(String.format("#%06X", c and 0xFFFFFF))
                hex.setSelection(hex.text.length)
                updatingHex = false
            }
            wheel.invalidate()
        }

        wheel.onChange = { refresh() }
        brightness.progress = (wheel.hsv[2] * 100).toInt()
        brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                wheel.hsv[2] = value / 100f
                refresh()
            }

            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })
        hex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (updatingHex) return
                val text = s.toString().removePrefix("#")
                if (text.length != 6) return
                val c = text.toLongOrNull(16) ?: return
                Color.colorToHSV((0xFF000000 or c).toInt(), wheel.hsv)
                brightness.progress = (wheel.hsv[2] * 100).toInt()
                refresh(fromHex = true)
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(wheel, MATCH_PARENT, WRAP_CONTENT)
            addView(TextView(context).apply {
                text = context.getString(R.string.look_brightness)
                setPadding(0, dp(12), 0, 0)
            })
            addView(brightness, MATCH_PARENT, WRAP_CONTENT)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
                addView(swatch, LinearLayout.LayoutParams(dp(48), dp(40)).apply { marginEnd = dp(16) })
                addView(hex, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            }, MATCH_PARENT, WRAP_CONTENT)
        }
        refresh()

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ -> picked(current()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
