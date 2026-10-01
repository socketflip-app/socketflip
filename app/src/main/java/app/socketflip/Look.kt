package app.socketflip

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** How the floating button looks. Colours are opaque ARGB; opacity is separate. */
data class Look(
    val downColor: Int,
    val upColor: Int,
    val ringColor: Int,
    val iconColor: Int,
    val idleAlpha: Float,
    val pressedAlpha: Float,
    val sizeDp: Int,
    /** Draw the arrow mirrored, so it turns anticlockwise. */
    val mirrorIcon: Boolean = false,
) {
    /** True when the icon would be hard to see on either button colour. */
    val lowContrast: Boolean
        get() = contrast(iconColor, downColor) < MIN_CONTRAST || contrast(iconColor, upColor) < MIN_CONTRAST

    companion object {
        /**
         * Opacity never goes below this: a button you cannot see is a button you
         * cannot find again. Reset lives in the app, never on the button itself.
         */
        const val MIN_ALPHA = 0.15f
        const val MIN_SIZE_DP = 36
        const val MAX_SIZE_DP = 96
        private const val MIN_CONTRAST = 3.0

        val DEFAULT = Look(
            downColor = 0xFF1E88E5.toInt(),
            upColor = 0xFF00897B.toInt(),
            ringColor = 0xFFFFA000.toInt(),
            iconColor = 0xFFFFFFFF.toInt(),
            idleAlpha = 0.8f,
            pressedAlpha = 1f,
            sizeDp = 52,
        )

        /** WCAG contrast ratio between two colours, 1 (same) to 21 (black on white). */
        fun contrast(a: Int, b: Int): Double {
            val la = luminance(a)
            val lb = luminance(b)
            return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
        }

        private fun luminance(c: Int): Double {
            fun ch(v: Int): Double {
                val s = v / 255.0
                return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
            }
            return 0.2126 * ch(Color.red(c)) + 0.7152 * ch(Color.green(c)) + 0.0722 * ch(Color.blue(c))
        }
    }
}
