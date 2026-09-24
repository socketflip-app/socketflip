package app.socketflip

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val REQ_VPN = 1
        private const val REQ_NOTIFY = 2
    }

    private lateinit var targetButton: Button
    private lateinit var status: TextView
    private lateinit var tipCard: LinearLayout
    private var askedNotify = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = dp(24)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        column.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
        })
        column.addView(TextView(this).apply {
            text = getString(R.string.intro)
            setPadding(0, dp(12), 0, dp(24))
        })
        targetButton = button { pickTarget() }
        column.addView(targetButton)
        column.addView(button(getString(R.string.show_button)) { start() })
        column.addView(button(getString(R.string.hide_button)) { stop() })
        status = TextView(this).apply { setPadding(0, dp(24), 0, 0) }
        column.addView(status)

        // Shown once, after the app has proved useful; "Not now" hides it for good.
        tipCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(32), 0, 0)
            addView(TextView(context).apply { text = getString(R.string.tip_prompt) })
            addView(button(getString(R.string.tip_button)) { openTip() })
            addView(button(getString(R.string.tip_not_now)) {
                Prefs.dismissTip(this@MainActivity)
                refresh()
            })
        }
        column.addView(tipCard)

        column.addView(TextView(this).apply {
            text = getString(R.string.tip_line)
            setPadding(0, dp(32), 0, 0)
        })
        column.addView(button(getString(R.string.tip_button)) { openTip() })

        column.addView(TextView(this).apply {
            text = getString(R.string.version_line, installedVersion())
            setPadding(0, dp(32), 0, 0)
        })
        column.addView(button(getString(R.string.check_updates)) { openUrl(Prefs.RELEASES_URL) })

        column.addView(TextView(this).apply {
            text = getString(R.string.licence_line)
            setPadding(0, dp(32), 0, 0)
        })
        column.addView(button(getString(R.string.source_code)) { openUrl(Prefs.SOURCE_URL) })
        setContentView(ScrollView(this).apply {
            addView(column, MATCH_PARENT, WRAP_CONTENT)
            // Android 15+ draws edge to edge; keep content clear of the system bars.
            setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (VpnService.prepare(this) == null) FlipVpnService.check(this)
    }

    private fun refresh() {
        val target = Prefs.target(this)
        targetButton.text = if (target == null) getString(R.string.no_target)
        else getString(R.string.choose_target, Prefs.label(this, target))
        status.text = getString(if (OverlayService.running) R.string.running else R.string.stopped)
        val prompt = Prefs.flips(this) >= Prefs.TIP_PROMPT_AFTER && !Prefs.tipDismissed(this)
        tipCard.visibility = if (prompt) View.VISIBLE else View.GONE
    }

    private fun openTip() {
        Prefs.dismissTip(this)
        openUrl(Prefs.TIP_URL)
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
        if (Prefs.target(this) == null) {
            pickTarget()
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
        VpnService.prepare(this)?.let {
            startActivityForResult(it, REQ_VPN)
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        status.text = getString(R.string.running)
    }

    private fun stop() {
        stopService(Intent(this, OverlayService::class.java))
        FlipVpnService.stop(this)
        status.text = getString(R.string.stopped)
    }

    private fun pickTarget() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName }
            .filter { it != packageName }
            .distinct()
            .map { it to Prefs.label(this, it) }
            .sortedBy { it.second.lowercase() }
        AlertDialog.Builder(this)
            .setTitle(R.string.no_target)
            .setItems(apps.map { it.second }.toTypedArray()) { _, i ->
                // A running tunnel still covers the old target; drop it first.
                if (FlipVpnService.isUp) FlipVpnService.stop(this)
                Prefs.setTarget(this, apps[i].first)
                refresh()
            }
            .show()
    }

    @Deprecated("Platform Activity API; no AndroidX in this app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == RESULT_OK) start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIFY) start()
    }

    private fun button(label: String = "", onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
