package app.socketflip

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var column: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = dp(24)
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        column.addView(TextView(this).apply {
            text = getString(R.string.settings)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        })
        behaviour()
        setContentView(ScrollView(this).apply {
            addView(column, MATCH_PARENT, WRAP_CONTENT)
            setOnApplyWindowInsetsListener { v, insets ->
                // getInsets() is Android 11+; the older getters still work on Android 10.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars())
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    v.setPadding(
                        insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                    )
                }
                insets
            }
        })
    }

    private fun behaviour() {
        heading(R.string.settings_behaviour)

        val label = TextView(this)
        val warning = TextView(this).apply {
            text = getString(R.string.settings_cooldown_short)
            setTextColor(0xFFEF6C00.toInt())
        }
        fun show(s: Int) {
            label.text = getString(R.string.settings_cooldown, s)
            warning.visibility = if (s < Prefs.COOLDOWN_DEFAULT_S) View.VISIBLE else View.GONE
        }
        column.addView(label)
        column.addView(SeekBar(this).apply {
            max = Prefs.COOLDOWN_MAX_S - Prefs.COOLDOWN_MIN_S
            progress = Prefs.cooldownSeconds(this@SettingsActivity) - Prefs.COOLDOWN_MIN_S
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    val s = value + Prefs.COOLDOWN_MIN_S
                    show(s)
                    if (fromUser) Prefs.setCooldownSeconds(this@SettingsActivity, s)
                }

                override fun onStartTrackingTouch(bar: SeekBar) {}
                override fun onStopTrackingTouch(bar: SeekBar) {}
            })
        }, MATCH_PARENT, WRAP_CONTENT)
        column.addView(warning)
        column.addView(TextView(this).apply {
            text = getString(R.string.settings_cooldown_help)
            setPadding(0, dp(4), 0, dp(12))
        })
        show(Prefs.cooldownSeconds(this))

        switch(R.string.settings_haptics, Prefs.haptics(this)) { Prefs.setHaptics(this, it) }
        switch(R.string.settings_hints, Prefs.hints(this)) { Prefs.setHints(this, it) }
    }

    private fun heading(res: Int) {
        column.addView(TextView(this).apply {
            text = getString(res)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    private fun switch(res: Int, on: Boolean, changed: (Boolean) -> Unit) {
        column.addView(Switch(this).apply {
            text = getString(res)
            isChecked = on
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, value -> changed(value) }
        }, MATCH_PARENT, WRAP_CONTENT)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
