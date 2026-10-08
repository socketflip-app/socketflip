package app.socketflip

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Colours of the emergency restart question. Pure, so the unit tests can check
 * every text and button against [Look.contrast].
 */
data class RestartColors(
    val background: Int,
    val text: Int,
    val restartFill: Int,
    val restartText: Int,
    val cancelFill: Int,
    val cancelText: Int,
    val cancelBorder: Int,
) {
    companion object {
        val LIGHT = RestartColors(
            background = 0xFFFFFFFF.toInt(),
            text = 0xFF1B1B1F.toInt(),
            restartFill = 0xFFD32F2F.toInt(),
            restartText = 0xFFFFFFFF.toInt(),
            cancelFill = 0xFFFFFFFF.toInt(),
            cancelText = 0xFF1B1B1F.toInt(),
            cancelBorder = 0xFF5F6168.toInt(),
        )
        val DARK = RestartColors(
            background = 0xFF1E1F23.toInt(),
            text = 0xFFFFFFFF.toInt(),
            restartFill = 0xFFD32F2F.toInt(),
            restartText = 0xFFFFFFFF.toInt(),
            cancelFill = 0xFF1E1F23.toInt(),
            cancelText = 0xFFFFFFFF.toInt(),
            cancelBorder = 0xFFB0B2B8.toInt(),
        )
    }
}

/**
 * The emergency restart question, built to be answered in a split second over a
 * game: a short title and exactly two big buttons, a filled red Restart and a
 * plain Cancel. Back and a tap outside the dialog both cancel. With several
 * target apps and none known to be on screen, it lists one Restart button per app
 * instead, still with Cancel. Drawn as an overlay so the app underneath stays put.
 *
 * Throws if the system refuses the overlay window; the caller then restarts nothing.
 */
object RestartDialog {

    fun show(context: Context, targets: List<String>, choice: String?) {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        val c = if (night) RestartColors.DARK else RestartColors.LIGHT
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val dialog = Dialog(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        fun button(text: String, filled: Boolean, onClick: () -> Unit) = Button(context, null, 0, 0).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            minHeight = dp(64)
            minimumHeight = dp(64)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(16), 0, dp(16), 0)
            setTextColor(if (filled) c.restartText else c.cancelText)
            val shape = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (filled) c.restartFill else c.cancelFill)
                if (!filled) setStroke(dp(2), c.cancelBorder)
            }
            background = RippleDrawable(ColorStateList.valueOf(0x33808080), shape, null)
            setOnClickListener {
                dialog.dismiss()
                onClick()
            }
        }

        val title = TextView(context).apply {
            text = if (choice != null) context.getString(R.string.restart_title, Prefs.label(context, choice))
            else context.getString(R.string.restart_pick_title)
            setTextColor(c.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 0, 0, dp(20))
        }
        val cancel = button(context.getString(R.string.restart_cancel), filled = false) {}

        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(c.background)
            }
            addView(title, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        if (choice != null) {
            // Cancel on the left, Restart on the right, never touching.
            val go = button(context.getString(R.string.restart_go), filled = true) { Restart.restart(context, choice) }
            box.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(cancel, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
                addView(go, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
            })
        } else {
            val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            for (pkg in targets) {
                list.addView(
                    button(context.getString(R.string.restart_app, Prefs.label(context, pkg)), filled = true) {
                        Restart.restart(context, pkg)
                    },
                    LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) }
                )
            }
            box.addView(ScrollView(context).apply { addView(list) }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            box.addView(cancel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(4) })
        }

        dialog.setContentView(box)
        dialog.window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Wide enough for big buttons, never wider than a comfortable reach, and the
            // same in portrait and over a landscape game.
            val screen = context.resources.displayMetrics.widthPixels
            setLayout(minOf(screen - dp(32), dp(440)), WRAP_CONTENT)
            setDimAmount(0.5f)
        }
        dialog.show()
    }
}
