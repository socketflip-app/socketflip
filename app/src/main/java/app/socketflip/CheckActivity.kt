package app.socketflip

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * One screen that answers "why is it not working?": every permission and setting
 * SocketFlip depends on, each marked OK, warning or problem, with a button to fix it.
 * "Copy report" puts the same list on the clipboard for a bug report.
 */
class CheckActivity : Activity() {

    private enum class Level(val mark: String, val color: Int) {
        OK("✓", 0xFF2E7D32.toInt()),
        WARN("!", 0xFFEF6C00.toInt()),
        BAD("✗", 0xFFC62828.toInt()),
        INFO("•", 0xFF757575.toInt()),
    }

    companion object {
        private const val REQ_VPN = 1
        // Same test as the main screen: a cancel this fast means Android closed the
        // consent screen unseen, because another app holds Always-on VPN.
        private const val UNSEEN_CANCEL_MS = 500L
    }

    /** [report] is what Copy report writes; it defaults to [detail] but never carries app names. */
    private class Item(
        val level: Level, val title: String, val detail: String,
        val report: String = detail, val fix: (() -> Unit)? = null,
    )

    private var consentAskedAt = 0L

    private lateinit var list: LinearLayout
    private var items: List<Item> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = Ui(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
        column.addView(ui.title(getString(R.string.check_title)))
        column.addView(ui.caption(getString(R.string.check_intro)).apply { setPadding(dp(4), dp(4), dp(4), dp(16)) })
        list = ui.card()
        column.addView(list)
        column.addView(ui.primaryButton(getString(R.string.check_copy)) { copyReport() })
        setContentView(ScrollView(this).apply {
            addView(column, MATCH_PARENT, WRAP_CONTENT)
            padForSystemBars()
        })
    }

    override fun onResume() {
        super.onResume()
        items = collect()
        list.removeAllViews()
        items.forEach { list.addView(row(it)) }
    }

    private fun collect(): List<Item> = buildList {
        val ticked = Prefs.checked(this@CheckActivity)
        val missing = ticked.filterNot { installed(it) }
        add(
            when {
                ticked.isEmpty() -> Item(Level.BAD, getString(R.string.check_target), getString(R.string.check_target_none)) {
                    finish()
                }
                missing.isNotEmpty() -> Item(
                    Level.WARN, getString(R.string.check_target),
                    getString(R.string.check_target_missing, missing.joinToString(", ")),
                    report = getString(R.string.check_target_report_missing, ticked.size, missing.size),
                ) { finish() }
                // The report is pasted into public bug reports, so it gives counts, never
                // which apps.
                else -> Item(
                    Level.OK, getString(R.string.check_target), Prefs.checkedLabels(this@CheckActivity),
                    report = getString(R.string.check_target_report_n, ticked.size),
                )
            }
        )

        add(
            if (Settings.canDrawOverlays(this@CheckActivity)) Item(Level.OK, getString(R.string.check_overlay), getString(R.string.check_allowed))
            else Item(Level.BAD, getString(R.string.check_overlay), getString(R.string.check_overlay_off)) {
                open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        )

        val notifyOk = getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        add(
            if (notifyOk) Item(Level.OK, getString(R.string.check_notify), getString(R.string.check_allowed))
            else Item(Level.WARN, getString(R.string.check_notify), getString(R.string.check_notify_off)) {
                open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        )

        // prepare() takes the VPN slot from any other VPN app once SocketFlip has
        // permission, so it is only asked when no other VPN is on.
        val otherVpn = FlipVpnService.otherVpnActive(this@CheckActivity)
        add(
            if (otherVpn) Item(Level.INFO, getString(R.string.check_vpn), getString(R.string.check_vpn_later))
            else VpnService.prepare(this@CheckActivity).let { consent ->
                if (consent == null) Item(Level.OK, getString(R.string.check_vpn), getString(R.string.check_allowed))
                else Item(Level.BAD, getString(R.string.check_vpn), getString(R.string.check_vpn_off)) { askVpn(consent) }
            }
        )

        add(
            when (alwaysOnApp()) {
                null -> Item(Level.INFO, getString(R.string.check_always_on), getString(R.string.check_always_on_unknown)) {
                    open(Intent(Settings.ACTION_VPN_SETTINGS))
                }
                packageName -> Item(Level.BAD, getString(R.string.check_always_on), getString(R.string.always_on_lockdown)) {
                    open(Intent(Settings.ACTION_VPN_SETTINGS))
                }
                "" -> Item(Level.OK, getString(R.string.check_always_on), getString(R.string.check_always_on_none))
                else -> Item(Level.WARN, getString(R.string.check_always_on), getString(R.string.check_always_on_other)) {
                    open(Intent(Settings.ACTION_VPN_SETTINGS))
                }
            }
        )

        add(
            if (otherVpn)
                Item(Level.WARN, getString(R.string.check_other_vpn), getString(R.string.check_other_vpn_on))
            else Item(Level.OK, getString(R.string.check_other_vpn), getString(R.string.check_other_vpn_off))
        )

        // Only a warning on the makers known to close background apps; the main screen
        // does not even ask on other phones, so a Pixel should not show a problem here.
        add(
            when {
                Battery.unrestricted(this@CheckActivity) ->
                    Item(Level.OK, getString(R.string.check_battery), getString(R.string.check_battery_ok))
                Battery.aggressiveMaker ->
                    Item(Level.WARN, getString(R.string.check_battery), getString(R.string.check_battery_limited)) {
                        Battery.request(this@CheckActivity)
                    }
                else -> Item(Level.INFO, getString(R.string.check_battery), getString(R.string.check_battery_info)) {
                    Battery.request(this@CheckActivity)
                }
            }
        )

        if (Prefs.onlyOverTarget(this@CheckActivity) || Prefs.dropOnLeave(this@CheckActivity)) {
            add(
                if (ForegroundWatcher.granted(this@CheckActivity))
                    Item(Level.OK, getString(R.string.check_usage), getString(R.string.check_usage_on))
                else Item(Level.WARN, getString(R.string.check_usage), getString(R.string.check_usage_off)) {
                    open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
        }

        add(
            Item(
                Level.INFO, getString(R.string.check_button),
                getString(if (OverlayService.running) R.string.check_on else R.string.check_off)
            )
        )
        add(
            Item(
                Level.INFO, getString(R.string.check_tunnel),
                getString(if (FlipVpnService.isUp) R.string.check_on else R.string.check_off)
            )
        )
    }

    private fun row(item: Item): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(8), 0, dp(8))
        addView(TextView(context).apply {
            text = item.level.mark
            setTextColor(item.level.color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            minWidth = dp(32)
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = item.title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(Ui(context).primaryText)
            })
            addView(TextView(context).apply {
                text = item.detail
                alpha = 0.75f
            })
            item.fix?.let { fix ->
                addView(Ui(context).textButton(getString(R.string.check_fix)) { fix() }, WRAP_CONTENT, WRAP_CONTENT)
            }
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    }

    private fun copyReport() {
        val report = buildString {
            appendLine("SocketFlip ${versionName()}")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            items.forEach { appendLine("${it.level.mark} ${it.title}: ${it.report}") }
        }
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(getString(R.string.check_title), report))
        // Android 13+ shows its own "copied" confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.check_copied, Toast.LENGTH_SHORT).show()
        }
    }

    /** Package holding Always-on VPN, "" for none, null when Android will not say. */
    private fun alwaysOnApp(): String? = try {
        Settings.Secure.getString(contentResolver, "always_on_vpn_app") ?: ""
    } catch (e: SecurityException) {
        null
    }

    private fun installed(pkg: String): Boolean = try {
        packageManager.getApplicationInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"

    /**
     * Android's VPN consent dialog only appears for a caller it can identify, and it
     * can only identify one that starts it for a result. A plain startActivity makes
     * the dialog close at once, so Fix looked like it did nothing (seen on a Samsung
     * S21, Android 14). onResume redraws the list when the dialog returns.
     */
    private fun askVpn(consent: Intent) {
        consentAskedAt = SystemClock.elapsedRealtime()
        try {
            startActivityForResult(consent, REQ_VPN)
        } catch (e: ActivityNotFoundException) {
            // No consent screen on this phone; the main screen's flow is the fallback.
        }
    }

    @Deprecated("Platform Activity API; no AndroidX in this app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_VPN || resultCode == RESULT_OK) return
        if (SystemClock.elapsedRealtime() - consentAskedAt >= UNSEEN_CANCEL_MS) return
        AlertDialog.Builder(this)
            .setTitle(R.string.other_always_on_title)
            .setMessage(R.string.other_always_on_text)
            .setPositiveButton(R.string.open_vpn_settings) { _, _ -> open(Intent(Settings.ACTION_VPN_SETTINGS)) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun open(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
