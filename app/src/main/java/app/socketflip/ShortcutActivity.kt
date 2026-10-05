package app.socketflip

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

/** Runs a home screen shortcut, then closes without drawing anything. */
class ShortcutActivity : Activity() {

    companion object {
        const val ACTION_FLIP = "app.socketflip.shortcut.FLIP"
        const val ACTION_SHOW = "app.socketflip.shortcut.SHOW"
        const val ACTION_HIDE = "app.socketflip.shortcut.HIDE"
        const val ACTION_LAUNCH = "app.socketflip.shortcut.LAUNCH"
        const val EXTRA_PACKAGE = "package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            ACTION_FLIP -> FlipVpnService.flip(this)
            ACTION_SHOW -> show()
            ACTION_LAUNCH -> intent.getStringExtra(EXTRA_PACKAGE)?.let { launchWithButton(it) }
            ACTION_HIDE -> {
                stopService(Intent(this, OverlayService::class.java))
                FlipVpnService.stop(this)
            }
        }
        // Theme.NoDisplay requires finishing before onResume.
        finish()
    }

    /**
     * The per-app home screen shortcut: make sure the app is ticked, show the
     * floating button, then open the app. If a permission is still missing, the
     * main screen opens instead so it can be sorted out first.
     */
    private fun launchWithButton(pkg: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch == null) {
            Toast.makeText(this, R.string.target_missing, Toast.LENGTH_SHORT).show()
            return
        }
        // A running tunnel only covers the apps ticked when it came up, so the next tap
        // would miss this one. Drop it now, while the app is not in front yet.
        if (FlipVpnService.isUp && pkg !in Prefs.checked(this)) FlipVpnService.stop(this)
        Prefs.addTarget(this, pkg)
        // With another VPN on, prepare() would switch it off, so skip that check once
        // the user has seen the main screen's warning; before that, show the warning.
        val otherVpn = FlipVpnService.otherVpnActive(this)
        val vpnOk = if (otherVpn) Prefs.otherVpnWarned(this) else VpnService.prepare(this) == null
        if (!Settings.canDrawOverlays(this) || !vpnOk) {
            finishSetup()
            return
        }
        if (!OverlayService.running) startForegroundService(Intent(this, OverlayService::class.java))
        startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
    }

    /** Starts the button if everything is already allowed; otherwise the main screen walks through it. */
    private fun show() {
        // prepare() takes the VPN slot from any other VPN app once SocketFlip has
        // permission, so with another VPN on, go to the main screen, which warns first.
        val ready = Prefs.checked(this).isNotEmpty() && Settings.canDrawOverlays(this) &&
            !FlipVpnService.otherVpnActive(this) && VpnService.prepare(this) == null
        if (ready) startForegroundService(Intent(this, OverlayService::class.java))
        else finishSetup()
    }

    /** The main screen, told to say why it opened instead of the shortcut's app. */
    private fun finishSetup() {
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_SHORTCUT, true))
    }
}
