package app.socketflip

import android.app.Activity
import android.graphics.Typeface
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var ui: Ui
    private lateinit var root: LinearLayout

    /** The card being filled; heading() starts a new one. */
    private lateinit var column: LinearLayout
    private lateinit var look: Look

    /** Everything on the page that shows the current look, redrawn after any change. */
    private val refreshers = mutableListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = Ui(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
        column = root
        root.addView(ui.title(getString(R.string.settings)).apply { setPadding(dp(4), 0, dp(4), dp(16)) })
        appearance()
        behaviour()
        setContentView(ScrollView(this).apply {
            addView(root, MATCH_PARENT, WRAP_CONTENT)
            padForSystemBars()
        })
    }

    private fun behaviour() {
        heading(R.string.settings_behaviour)

        val label = TextView(this).apply { setTextColor(ui.primaryText) }
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
        column.addView(ui.caption(getString(R.string.settings_cooldown_help)))
        show(Prefs.cooldownSeconds(this))

        switch(R.string.settings_haptics, Prefs.haptics(this)) { Prefs.setHaptics(this, it) }
        switch(R.string.settings_hints, Prefs.hints(this)) { Prefs.setHints(this, it) }
        switch(R.string.settings_emergency, Prefs.emergencyRestart(this)) { Prefs.setEmergencyRestart(this, it) }
        column.addView(ui.caption(getString(R.string.settings_emergency_help)))
        // Force stop lives here, not in the restart question, which keeps to two choices.
        column.addView(ui.textButton(getString(R.string.restart_force)) { openAppInfo() }, WRAP_CONTENT, WRAP_CONTENT)

        // The two settings that need Usage access sit together, with the warning
        // directly under them, so it is obvious which settings it is about.
        heading(R.string.settings_follow_heading)
        switch(R.string.settings_only_over_target, Prefs.onlyOverTarget(this)) {
            Prefs.setOnlyOverTarget(this, it)
            if (it) askForUsageAccess()
            refreshUsageNote()
        }
        switch(R.string.settings_drop_on_leave, Prefs.dropOnLeave(this)) {
            Prefs.setDropOnLeave(this, it)
            if (it) askForUsageAccess()
            refreshUsageNote()
        }
        usageText = TextView(this).apply { setTextColor(0xFFEF6C00.toInt()) }
        usageNote = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
            addView(usageText)
            addView(Button(context).apply {
                text = getString(R.string.settings_usage_allow)
                setOnClickListener { openUsageAccess() }
            }, WRAP_CONTENT, WRAP_CONTENT)
        }
        column.addView(usageNote)

        heading(R.string.settings_automation_heading)
        switch(R.string.settings_automation, Prefs.automation(this)) { Prefs.setAutomation(this, it) }
        column.addView(ui.caption(getString(R.string.settings_automation_help)).apply { setTextIsSelectable(true) })
        val token = Prefs.automationToken(this)
        column.addView(ui.caption(getString(R.string.settings_automation_token, token)).apply { setTextIsSelectable(true) })
        column.addView(ui.textButton(getString(R.string.settings_automation_copy)) {
            getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText(getString(R.string.settings_automation_heading), token))
            // Android 13+ shows its own "copied" confirmation.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(this@SettingsActivity, R.string.settings_automation_copied, Toast.LENGTH_SHORT).show()
            }
        }, WRAP_CONTENT, WRAP_CONTENT)
    }

    private var usageNote: LinearLayout? = null
    private var usageText: TextView? = null

    override fun onResume() {
        super.onResume()
        refreshUsageNote()
        // Coming back from the Usage access screen: the overlay only rechecks on a
        // settings change, so nudge it.
        Prefs.touch(this)
    }

    /** Names the setting that cannot work, rather than pointing vaguely "above". */
    private fun refreshUsageNote() {
        val on = listOfNotNull(
            getString(R.string.settings_only_over_target).takeIf { Prefs.onlyOverTarget(this) },
            getString(R.string.settings_drop_on_leave).takeIf { Prefs.dropOnLeave(this) },
        )
        val missing = on.isNotEmpty() && !ForegroundWatcher.granted(this)
        usageNote?.visibility = if (missing) View.VISIBLE else View.GONE
        if (missing) {
            usageText?.text = if (on.size == 1) getString(R.string.settings_usage_missing_one, on[0])
            else getString(R.string.settings_usage_missing_both)
        }
    }

    private fun askForUsageAccess() {
        if (ForegroundWatcher.granted(this)) return
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_usage_title)
            .setMessage(R.string.settings_usage_text)
            .setPositiveButton(R.string.settings_usage_open) { _, _ -> openUsageAccess() }
            .setNegativeButton(R.string.tip_not_now, null)
            .show()
    }

    private fun openUsageAccess() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                // No such screen on this phone.
            }
        }
    }

    private fun appearance() {
        look = Prefs.look(this)
        heading(R.string.settings_appearance)

        // Live preview on a mid-grey panel, so the opacity settings are visible.
        val previews = listOf(
            R.string.look_preview_ready to FlipButtonView.Preview(up = false, ringFraction = 0f),
            R.string.look_preview_up to FlipButtonView.Preview(up = true, ringFraction = 0f),
            R.string.look_preview_cooldown to FlipButtonView.Preview(up = true, ringFraction = 0.6f),
            R.string.look_preview_tapped to FlipButtonView.Preview(up = false, ringFraction = 1f, pressed = true),
        )
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(0xFF808080.toInt())
            }
            setPadding(dp(4), dp(12), dp(4), dp(12))
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
            })
            panel.addView(cell, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            refreshers += {
                view.look = look
                val size = dp(look.sizeDp)
                view.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            }
        }
        column.addView(panel, MATCH_PARENT, WRAP_CONTENT)

        val contrast = ui.caption(getString(R.string.look_low_contrast)).apply {
            setTextColor(ui.warning)
            setPadding(0, dp(8), 0, 0)
        }
        column.addView(contrast)
        refreshers += { contrast.visibility = if (look.lowContrast) View.VISIBLE else View.GONE }

        skins()

        heading(R.string.look_customise)
        colourRow(R.string.look_down, { it.downColor }) { l, c -> l.copy(downColor = c) }
        colourRow(R.string.look_up, { it.upColor }) { l, c -> l.copy(upColor = c) }
        colourRow(R.string.look_ring, { it.ringColor }) { l, c -> l.copy(ringColor = c) }
        colourRow(R.string.look_icon, { it.iconColor }) { l, c -> l.copy(iconColor = c) }

        val mirror = Switch(this).apply {
            text = getString(R.string.look_mirror)
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, on -> if (on != look.mirrorIcon) update(look.copy(mirrorIcon = on)) }
        }
        column.addView(mirror, MATCH_PARENT, WRAP_CONTENT)
        refreshers += { mirror.isChecked = look.mirrorIcon }

        val minPercent = (Look.MIN_ALPHA * 100).toInt()
        slider(R.string.look_idle_alpha, minPercent, 100, { (it.idleAlpha * 100).toInt() }) { l, v ->
            l.copy(idleAlpha = v / 100f)
        }
        slider(R.string.look_pressed_alpha, minPercent, 100, { (it.pressedAlpha * 100).toInt() }) { l, v ->
            l.copy(pressedAlpha = v / 100f)
        }
        slider(R.string.look_size, Look.MIN_SIZE_DP, Look.MAX_SIZE_DP, { it.sizeDp }) { l, v -> l.copy(sizeDp = v) }

        switch(R.string.look_snap, Prefs.snapToEdge(this)) { Prefs.setSnapToEdge(this, it) }
        column.addView(ui.textButton(getString(R.string.look_reset)) { update(Look.DEFAULT) }, WRAP_CONTENT, WRAP_CONTENT)

        refreshers.forEach { it() }
    }

    /** A sideways-scrolling row of ready-made looks; tap one to use it. */
    private fun skins() {
        heading(R.string.look_skins)
        column.addView(ui.caption(getString(R.string.look_skins_help)))
        val strip = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Skins.ALL.forEach { skin ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
                isClickable = true
                setOnClickListener { update(skin.look) }
            }
            val swatch = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(0xFF1B1B1F.toInt())
                }
            }
            val size = dp(40)
            // Tunnel down and tunnel up side by side, so a skin shows both of its states.
            val pair = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(10), dp(8), dp(10))
                addView(FlipButtonView(context).apply {
                    look = skin.look.copy(idleAlpha = 1f)
                    preview = FlipButtonView.Preview(up = false, ringFraction = 0f)
                }, LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(6) })
                addView(FlipButtonView(context).apply {
                    look = skin.look.copy(idleAlpha = 1f)
                    preview = FlipButtonView.Preview(up = true, ringFraction = 0.6f)
                }, LinearLayout.LayoutParams(size, size))
            }
            swatch.addView(pair)
            tile.addView(swatch)
            val name = TextView(this).apply {
                text = skin.name
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, 0)
            }
            tile.addView(name)
            strip.addView(tile)
            refreshers += {
                val selected = look == skin.look
                (swatch.background as GradientDrawable).setStroke(if (selected) dp(3) else 0, ui.accent)
                name.setTextColor(if (selected) ui.accent else ui.secondaryText)
                name.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            }
        }
        column.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(strip)
        }, MATCH_PARENT, WRAP_CONTENT)
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
                setTextColor(ui.primaryText)
            })
            setOnClickListener {
                ColorPicker.show(this@SettingsActivity, getString(res), get(look)) { update(set(look, it)) }
            }
        }
        column.addView(row, MATCH_PARENT, WRAP_CONTENT)
        refreshers += { swatch.setColor(get(look)) }
    }

    private fun slider(res: Int, min: Int, max: Int, get: (Look) -> Int, set: (Look, Int) -> Look) {
        val label = TextView(this).apply {
            setPadding(0, dp(12), 0, 0)
            setTextColor(ui.primaryText)
        }
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
        val card = ui.card()
        card.addView(ui.sectionTitle(getString(res)))
        root.addView(card)
        column = card
    }

    private fun switch(res: Int, on: Boolean, changed: (Boolean) -> Unit) {
        column.addView(Switch(this).apply {
            text = getString(res)
            isChecked = on
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, value -> changed(value) }
        }, MATCH_PARENT, WRAP_CONTENT)
    }

    /** App info (with Force stop) for the target app, asking which one when several are ticked. */
    private fun openAppInfo() {
        val targets = Prefs.checked(this).filter { packageManager.getLaunchIntentForPackage(it) != null }
        when (targets.size) {
            0 -> Toast.makeText(this, R.string.target_unset, Toast.LENGTH_SHORT).show()
            1 -> Restart.appInfo(this, targets[0])
            else -> AlertDialog.Builder(this)
                .setTitle(R.string.restart_force_pick)
                .setItems(targets.map { Prefs.label(this, it) }.toTypedArray()) { _, i -> Restart.appInfo(this, targets[i]) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
