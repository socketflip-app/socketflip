package app.socketflip

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var column: LinearLayout
    private lateinit var look: Look

    /** Everything on the page that shows the current look, redrawn after any change. */
    private val refreshers = mutableListOf<() -> Unit>()

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
        appearance()
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

    private fun appearance() {
        look = Prefs.look(this)
        heading(R.string.settings_appearance)

        // Live preview on a mid-grey card, so the opacity settings are visible.
        val previews = listOf(
            R.string.look_preview_ready to FlipButtonView.Preview(up = false, ringFraction = 0f),
            R.string.look_preview_up to FlipButtonView.Preview(up = true, ringFraction = 0f),
            R.string.look_preview_cooldown to FlipButtonView.Preview(up = true, ringFraction = 0.6f),
            R.string.look_preview_tapped to FlipButtonView.Preview(up = false, ringFraction = 1f, pressed = true),
        )
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF808080.toInt())
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        previews.forEach { (label, state) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val box = FrameLayout(this)
            val view = FlipButtonView(this).apply { preview = state }
            box.addView(view, FrameLayout.LayoutParams(0, 0, Gravity.CENTER))
            cell.addView(box, LinearLayout.LayoutParams(MATCH_PARENT, dp(Look.MAX_SIZE_DP)))
            cell.addView(TextView(this).apply {
                text = getString(label)
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
            })
            card.addView(cell, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            refreshers += {
                view.look = look
                val size = dp(look.sizeDp)
                view.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            }
        }
        column.addView(card, MATCH_PARENT, WRAP_CONTENT)

        val contrast = TextView(this).apply {
            text = getString(R.string.look_low_contrast)
            setTextColor(0xFFEF6C00.toInt())
            setPadding(0, dp(8), 0, 0)
        }
        column.addView(contrast)
        refreshers += { contrast.visibility = if (look.lowContrast) View.VISIBLE else View.GONE }

        colourRow(R.string.look_down, { it.downColor }) { l, c -> l.copy(downColor = c) }
        colourRow(R.string.look_up, { it.upColor }) { l, c -> l.copy(upColor = c) }
        colourRow(R.string.look_ring, { it.ringColor }) { l, c -> l.copy(ringColor = c) }
        colourRow(R.string.look_icon, { it.iconColor }) { l, c -> l.copy(iconColor = c) }

        val minPercent = (Look.MIN_ALPHA * 100).toInt()
        slider(R.string.look_idle_alpha, minPercent, 100, { (it.idleAlpha * 100).toInt() }) { l, v ->
            l.copy(idleAlpha = v / 100f)
        }
        slider(R.string.look_pressed_alpha, minPercent, 100, { (it.pressedAlpha * 100).toInt() }) { l, v ->
            l.copy(pressedAlpha = v / 100f)
        }
        slider(R.string.look_size, Look.MIN_SIZE_DP, Look.MAX_SIZE_DP, { it.sizeDp }) { l, v -> l.copy(sizeDp = v) }

        switch(R.string.look_snap, Prefs.snapToEdge(this)) { Prefs.setSnapToEdge(this, it) }

        column.addView(TextView(this).apply {
            text = getString(R.string.look_presets)
            setPadding(0, dp(16), 0, dp(4))
        })
        column.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            listOf(
                R.string.look_preset_default to Look.DEFAULT,
                R.string.look_preset_subtle to Look.SUBTLE,
                R.string.look_preset_contrast to Look.HIGH_CONTRAST,
            ).forEach { (label, preset) ->
                addView(Button(context).apply {
                    text = getString(label)
                    setOnClickListener { update(preset) }
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            }
        }, MATCH_PARENT, WRAP_CONTENT)
        column.addView(Button(this).apply {
            text = getString(R.string.look_reset)
            setOnClickListener { update(Look.DEFAULT) }
        })

        refreshers.forEach { it() }
    }

    private fun update(next: Look) {
        look = next
        Prefs.setLook(this, next)
        refreshers.forEach { it() }
    }

    private fun colourRow(res: Int, get: (Look) -> Int, set: (Look, Int) -> Look) {
        val swatch = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(1), 0xFF808080.toInt())
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            isClickable = true
            addView(View(context).apply { background = swatch }, LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                marginEnd = dp(16)
            })
            addView(TextView(context).apply {
                text = getString(res)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            })
            setOnClickListener {
                ColorPicker.show(this@SettingsActivity, getString(res), get(look)) { update(set(look, it)) }
            }
        }
        column.addView(row, MATCH_PARENT, WRAP_CONTENT)
        refreshers += { swatch.setColor(get(look)) }
    }

    private fun slider(res: Int, min: Int, max: Int, get: (Look) -> Int, set: (Look, Int) -> Look) {
        val label = TextView(this).apply { setPadding(0, dp(12), 0, 0) }
        val bar = SeekBar(this).apply {
            this.max = max - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    if (fromUser) update(set(look, value + min))
                }

                override fun onStartTrackingTouch(bar: SeekBar) {}
                override fun onStopTrackingTouch(bar: SeekBar) {}
            })
        }
        column.addView(label)
        column.addView(bar, MATCH_PARENT, WRAP_CONTENT)
        refreshers += {
            val v = get(look)
            label.text = getString(res, v)
            bar.progress = v - min
        }
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
