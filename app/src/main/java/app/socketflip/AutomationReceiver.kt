package app.socketflip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Lets automation apps (Tasker, MacroDroid, Key Mapper and the like) trigger a flip.
 * Off unless the user switches it on in Settings, and even then a broadcast must
 * carry the per-install token shown in Settings, because any app on the phone can
 * send it. The cooldown applies to FLIP and STOP alike, so it cannot be used to
 * hammer the target app.
 *
 * Why a token and not a custom permission: Tasker, MacroDroid and Key Mapper cannot
 * declare arbitrary permissions, so a permission would lock out exactly the apps
 * this is for. Please do not "fix" it that way.
 *
 * Rejected broadcasts are only logged: a toast would let any app pop up SocketFlip
 * messages over the game.
 */
class AutomationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FLIP = "app.socketflip.FLIP"
        const val ACTION_STOP = "app.socketflip.STOP"
        const val EXTRA_TOKEN = "token"
        private const val TAG = "SocketFlip"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.automation(context)) {
            Log.i(TAG, "automation broadcast ignored: switched off")
            return
        }
        if (!Prefs.tokenMatches(intent.getStringExtra(EXTRA_TOKEN), Prefs.automationToken(context))) {
            Log.w(TAG, "automation broadcast ignored: missing or wrong token")
            return
        }
        when (intent.action) {
            ACTION_FLIP -> FlipVpnService.flip(context)
            ACTION_STOP -> {
                // Taking the tunnel down disconnects the target just like a flip.
                if (FlipVpnService.cooldownLeft(context) > 0f) {
                    Toast.makeText(context, R.string.cooldown, Toast.LENGTH_SHORT).show()
                } else {
                    FlipVpnService.stop(context)
                }
            }
        }
    }
}
