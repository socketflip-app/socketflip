package app.socketflip

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val REQ_VPN = 1
        private const val REQ_NOTIFY = 2
        // Android closes its VPN consent screen unseen when another app holds
        // Always-on VPN; a "cancel" this fast means nobody saw a dialog.
        private const val UNSEEN_CANCEL_MS = 500L
    }

    private lateinit var ui: Ui
    private lateinit var targetList: LinearLayout
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private lateinit var tipCard: LinearLayout
    private lateinit var tipDone: TextView
    private var askedNotify = false
    private var consentAskedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = Ui(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }

        column.addView(ui.title(getString(R.string.app_name)))
        column.addView(ui.caption(getString(R.string.intro_short)).apply { setPadding(dp(4), dp(4), dp(4), dp(4)) })
        status = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(4), dp(4), dp(4), dp(16))
        }
        column.addView(status)

        column.addView(ui.card().apply {
            addView(ui.sectionTitle(getString(R.string.targets_title)))
            targetList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            addView(targetList)
            addView(ui.caption(getString(R.string.targets_help)))
            addView(ui.textButton(getString(R.string.targets_add_plus)) { pickTarget() }, WRAP_CONTENT, WRAP_CONTENT)
        })

        toggle = ui.primaryButton("") { if (OverlayService.running) stop() else start() }
        column.addView(toggle)
        column.addView(ui.row(
            ui.textButton(getString(R.string.settings)) { startActivity(Intent(this, SettingsActivity::class.java)) },
            ui.textButton(getString(R.string.check_setup)) { startActivity(Intent(this, CheckActivity::class.java)) },
        ).apply { setPadding(0, 0, 0, dp(12)) })

        // Shown once, after the app has proved useful; "Not now" hides it for good.
        tipCard = ui.card().apply {
            addView(ui.sectionTitle(getString(R.string.tip_title)))
            // Filled in by refresh(): a concrete number says more than a generic ask.
            tipDone = TextView(context).apply { setPadding(0, 0, 0, dp(8)) }
            addView(tipDone)
            addView(ui.caption(getString(R.string.tip_prompt)))
            addView(ui.row(
                ui.textButton(getString(R.string.tip_button)) { openTip() },
                ui.textButton(getString(R.string.tip_not_now)) {
                    Prefs.dismissTip(this@MainActivity)
                    refresh()
                },
            ))
        }
        column.addView(tipCard)

        column.addView(ui.card().apply {
            addView(ui.sectionTitle(getString(R.string.about_title)))
            addView(ui.caption(getString(R.string.tip_line)))
            addView(ui.caption(getString(R.string.version_line, installedVersion())))
            addView(ui.caption(getString(R.string.licence_line)))
            addView(ui.row(
                ui.textButton(getString(R.string.tip_button)) { openTip() },
                ui.textButton(getString(R.string.check_updates_short)) { openUrl(Prefs.RELEASES_URL) },
                ui.textButton(getString(R.string.source_short)) { openUrl(Prefs.SOURCE_URL) },
            ))
        })

        setContentView(ScrollView(this).apply {
            addView(column, MATCH_PARENT, WRAP_CONTENT)
            // Android 15+ draws edge to edge; keep content clear of the system bars.
            padForSystemBars()
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
        // prepare() is not a read-only question: once SocketFlip has permission it takes
        // the VPN slot from any other VPN app. So never call it while another VPN is on.
        if (!FlipVpnService.otherVpnActive(this) && VpnService.prepare(this) == null) FlipVpnService.check(this)
    }

    private fun refresh() {
        val ticked = Prefs.checked(this)
        targetList.removeAllViews()
        Prefs.targets(this).forEach { targetList.addView(targetRow(it, it in ticked)) }
        showRunning(OverlayService.running)
        val flips = Prefs.flips(this)
        val prompt = flips >= Prefs.TIP_PROMPT_AFTER && !Prefs.tipDismissed(this)
        tipCard.visibility = if (prompt) View.VISIBLE else View.GONE
        if (prompt) tipDone.text = tipDoneText(ticked.singleOrNull(), flips)
    }

    /** Status line with a coloured dot, and the main button saying what it will do. */
    private fun showRunning(on: Boolean) {
        status.text = getString(if (on) R.string.status_on else R.string.status_off)
        status.setTextColor(if (on) 0xFF43A047.toInt() else ui.secondaryText)
        toggle.text = getString(if (on) R.string.hide_button else R.string.show_button)
    }

    /** What SocketFlip has done so far, in the user's own numbers. */
    private fun tipDoneText(target: String?, flips: Int): String =
        if (target == null) resources.getQuantityString(R.plurals.tip_count, flips, flips)
        else resources.getQuantityString(R.plurals.tip_count_app, flips, Prefs.label(this, target), flips)

    /** Two ways to give, one tap each: a card or wallet payment on Stripe, or Ko-fi. */
    private fun openTip() {
        Prefs.dismissTip(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.tip_choose)
            .setItems(arrayOf(getString(R.string.tip_stripe), getString(R.string.tip_kofi))) { _, which ->
                openUrl(if (which == 0) Prefs.TIP_URL else Prefs.KOFI_URL)
            }
            .show()
    }

    /** Opens a page in the browser; SocketFlip itself never touches the network. */
    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            status.text = url
        }
    }

    private fun installedVersion(): String =
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"

    /** Walks the user through each missing permission, then shows the button. */
    private fun start() {
        if (Prefs.targets(this).isEmpty()) {
            pickTarget()
            return
        }
        if (Prefs.checked(this).isEmpty()) {
            status.text = getString(R.string.target_unset)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            status.text = getString(R.string.need_overlay)
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotify &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotify = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFY)
            return
        }
        // Android runs one VPN at a time. Say so once, before anything switches off
        // someone's privacy or work VPN. This must come before prepare(), which already
        // takes the VPN slot away from the other app.
        if (FlipVpnService.otherVpnActive(this) && !Prefs.otherVpnWarned(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.other_vpn_title)
                .setMessage(R.string.other_vpn_text)
                .setPositiveButton(R.string.other_vpn_ok) { _, _ ->
                    Prefs.setOtherVpnWarned(this)
                    start()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }
        VpnService.prepare(this)?.let {
            consentAskedAt = SystemClock.elapsedRealtime()
            try {
                startActivityForResult(it, REQ_VPN)
            } catch (e: ActivityNotFoundException) {
                alwaysOnDialog()
            }
            return
        }
        // Ask once, and only on phones whose makers are known to close background apps.
        if (Battery.aggressiveMaker && !Battery.unrestricted(this) && !Prefs.batteryAsked(this)) {
            Prefs.setBatteryAsked(this)
            AlertDialog.Builder(this)
                .setTitle(R.string.battery_title)
                .setMessage(R.string.battery_text)
                .setPositiveButton(R.string.battery_allow) { _, _ -> Battery.request(this) }
                .setNegativeButton(R.string.tip_not_now, null)
                .setOnDismissListener { start() }
                .show()
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        showRunning(true)
    }

    private fun stop() {
        stopService(Intent(this, OverlayService::class.java))
        FlipVpnService.stop(this)
        showRunning(false)
    }

    /**
     * One row per target: a checkbox (every ticked app is reconnected by each tap),
     * its cooldown, and plain Cooldown and Remove buttons.
     */
    private fun targetRow(pkg: String, ticked: Boolean): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val name = Prefs.label(this@MainActivity, pkg)
        val own = Prefs.targetCooldownSeconds(this@MainActivity, pkg)
        val cooldown = if (own == null) getString(R.string.target_cooldown_line_default, Prefs.cooldownSeconds(context))
        else getString(R.string.target_cooldown_line, own)
        addView(CheckBox(context).apply {
            text = SpannableStringBuilder(name).append("\n").append(
                cooldown,
                RelativeSizeSpan(0.85f),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            ).apply {
                setSpan(ForegroundColorSpan(ui.secondaryText), name.length + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            isChecked = ticked
            setOnCheckedChangeListener { _, on -> setTicked(pkg, on) }
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        addView(small(getString(R.string.target_cooldown_button)) { targetCooldown(pkg, name) })
        addView(small(getString(R.string.target_remove)) { confirmRemove(pkg, name) })
    }

    private fun small(label: String, onClick: () -> Unit) = ui.textButton(label, onClick)

    private fun setTicked(pkg: String, on: Boolean) {
        // A running tunnel covers the old set of apps; drop it so the next tap uses the new one.
        if (FlipVpnService.isUp) FlipVpnService.stop(this)
        Prefs.setChecked(this, pkg, on)
        refresh()
    }

    private fun confirmRemove(pkg: String, name: String) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.target_remove_confirm, name))
            .setPositiveButton(R.string.target_remove) { _, _ ->
                if (pkg in Prefs.checked(this) && FlipVpnService.isUp) FlipVpnService.stop(this)
                Prefs.removeTarget(this, pkg)
                refresh()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Per-app cooldown: the Settings value, or one of a few fixed choices. */
    private fun targetCooldown(pkg: String, name: String) {
        val choices = listOf<Int?>(null, 3, 5, 10, 15, 20, 30, 45, 60)
        val labels = choices.map {
            if (it == null) getString(R.string.target_cooldown_default) else getString(R.string.target_cooldown_value, it)
        }
        val current = choices.indexOf(Prefs.targetCooldownSeconds(this, pkg)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.target_cooldown) + ": " + name)
            .setSingleChoiceItems(labels.toTypedArray(), current) { dialog, which ->
                Prefs.setTargetCooldownSeconds(this, pkg, choices[which])
                dialog.dismiss()
                refresh()
            }
            .show()
    }

    private fun pickTarget() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val existing = Prefs.targets(this)
        val apps = packageManager.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName }
            .filter { it != packageName && it !in existing }
            .distinct()
            .map { it to Prefs.label(this, it) }
            .sortedBy { it.second.lowercase() }
        AlertDialog.Builder(this)
            .setTitle(R.string.targets_add)
            .setItems(apps.map { it.second }.toTypedArray()) { _, i ->
                if (FlipVpnService.isUp) FlipVpnService.stop(this)
                Prefs.addTarget(this, apps[i].first)
                refresh()
            }
            .show()
    }

    @Deprecated("Platform Activity API; no AndroidX in this app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_VPN) return
        when {
            resultCode == RESULT_OK -> start()
            SystemClock.elapsedRealtime() - consentAskedAt < UNSEEN_CANCEL_MS -> alwaysOnDialog()
            else -> AlertDialog.Builder(this)
                .setTitle(R.string.consent_cancelled_title)
                .setMessage(R.string.consent_cancelled_text)
                .setPositiveButton(R.string.consent_try_again) { _, _ -> start() }
                .setNeutralButton(R.string.consent_why_safe) { _, _ -> openUrl(Prefs.SAFETY_URL) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    /** The consent screen closed before anyone could see it: another app holds Always-on VPN. */
    private fun alwaysOnDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.other_always_on_title)
            .setMessage(R.string.other_always_on_text)
            .setPositiveButton(R.string.open_vpn_settings) { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                } catch (e: ActivityNotFoundException) {
                    // No VPN settings screen; the message already says where to look.
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIFY) start()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
