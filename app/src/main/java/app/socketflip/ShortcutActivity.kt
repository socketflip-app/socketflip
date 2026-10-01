package app.socketflip

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings

/** Runs a home screen shortcut, then closes without drawing anything. */
class ShortcutActivity : Activity() {

    companion object {
        const val ACTION_FLIP = "app.socketflip.shortcut.FLIP"
        const val ACTION_SHOW = "app.socketflip.shortcut.SHOW"
        const val ACTION_HIDE = "app.socketflip.shortcut.HIDE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent?.action) {
            ACTION_FLIP -> FlipVpnService.flip(this)
            ACTION_SHOW -> show()
            ACTION_HIDE -> {
                stopService(Intent(this, OverlayService::class.java))
                FlipVpnService.stop(this)
            }
        }
        // Theme.NoDisplay requires finishing before onResume.
        finish()
    }

    /** Starts the button if everything is already allowed; otherwise the main screen walks through it. */
    private fun show() {
        // prepare() takes the VPN slot from any other VPN app once SocketFlip has
        // permission, so with another VPN on, go to the main screen, which warns first.
        val ready = Prefs.checked(this).isNotEmpty() && Settings.canDrawOverlays(this) &&
            !FlipVpnService.otherVpnActive(this) && VpnService.prepare(this) == null
        if (ready) startForegroundService(Intent(this, OverlayService::class.java))
        else startActivity(Intent(this, MainActivity::class.java))
    }
}
