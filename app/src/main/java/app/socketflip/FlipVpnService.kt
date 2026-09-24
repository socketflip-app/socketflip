package app.socketflip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.net.Inet4Address
import java.net.InetAddress

/**
 * A per-app VPN that routes nothing.
 *
 * Whenever a VPN covering an app comes up or goes down, Android closes that app's
 * open sockets. The app sees its connection drop and reconnects at once, over the
 * normal network, because the tunnel only claims one unused /32 address.
 *
 * Every flip is exactly ONE state change (up, or down), so every tap is exactly one
 * clean disconnect. Bringing the tunnel up and immediately down again would hit the
 * app twice, and a reconnect that lands between the two can wedge.
 */
class FlipVpnService : VpnService() {

    companion object {
        const val ACTION_FLIP = "app.socketflip.action.FLIP"
        const val ACTION_STOP = "app.socketflip.action.STOP"
        const val ACTION_CHECK = "app.socketflip.action.CHECK"

        private const val TAG = "SocketFlip"
        // A second disconnect while the target is still reconnecting can leave it
        // stuck on its reconnect screen, so flips are spaced at least this far apart.
        private const val COOLDOWN_MS = 10_000L

        // A private /32 pair the target app will never talk to, so nothing is routed.
        private const val TUN_ADDRESS = "10.111.222.1"
        private const val TUN_ROUTE = "10.111.222.2"
        private const val WARN_CHANNEL = "warnings"
        private const val WARN_ID = 2
        private val FALLBACK_DNS = listOf("1.1.1.1", "9.9.9.9")

        @Volatile private var tun: ParcelFileDescriptor? = null
        @Volatile private var lastFlip = 0L

        val isUp: Boolean get() = tun != null

        fun flip(context: Context) = send(context, ACTION_FLIP)

        fun stop(context: Context) = send(context, ACTION_STOP)

        /** Warns if SocketFlip has been made the Always-on VPN (see [warnIfAlwaysOn]). */
        fun check(context: Context) = send(context, ACTION_CHECK)

        private fun send(context: Context, action: String) {
            try {
                context.startService(Intent(context, FlipVpnService::class.java).setAction(action))
            } catch (e: IllegalStateException) {
                Log.w(TAG, "could not start service for $action", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Never crash: a crash dialog would land on top of the target app.
        try {
            when (intent?.action) {
                ACTION_FLIP -> flipOnce()
                ACTION_STOP -> down()
            }
            warnIfAlwaysOn()
        } catch (e: Exception) {
            Log.e(TAG, "flip failed", e)
            toast(getString(R.string.flip_failed, e.message ?: e.javaClass.simpleName))
        }
        if (tun == null) stopSelf()
        return START_NOT_STICKY
    }

    private fun flipOnce() {
        val now = SystemClock.elapsedRealtime()
        if (lastFlip != 0L && now - lastFlip < COOLDOWN_MS) {
            toast(getString(R.string.cooldown))
            return
        }
        lastFlip = now
        if (tun != null) down() else up()
    }

    private fun up() {
        if (prepare(this) != null) {
            toast(getString(R.string.need_vpn))
            return
        }
        val target = Prefs.target(this) ?: return
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(TUN_ADDRESS, 32)
            .addRoute(TUN_ROUTE, 32)
            .setMtu(1280)
            .setBlocking(false)
            .setMetered(false)
        try {
            builder.addAllowedApplication(target)
        } catch (e: PackageManager.NameNotFoundException) {
            toast(getString(R.string.target_missing))
            return
        }
        dnsServers().forEach { builder.addDnsServer(it) }
        tun = builder.establish()
        Log.i(TAG, "tunnel up for $target: ${tun != null}")
    }

    private fun down() {
        tun?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.w(TAG, "close failed", e)
            }
            Log.i(TAG, "tunnel down")
        }
        tun = null
    }

    /**
     * The tunnel must hand the app a resolver, or its lookups fail while the tunnel
     * is up. Reuse the current network's servers (they are reached outside the
     * tunnel, since it routes nothing), falling back to public resolvers.
     */
    private fun dnsServers(): List<InetAddress> {
        val current = try {
            val cm = getSystemService(ConnectivityManager::class.java)
            cm?.activeNetwork?.let { cm.getLinkProperties(it)?.dnsServers }.orEmpty()
                .filter { it is Inet4Address && !it.isLinkLocalAddress }
        } catch (e: SecurityException) {
            emptyList()
        }
        return current.ifEmpty { FALLBACK_DNS.map { InetAddress.getByName(it) } }
    }

    /**
     * SocketFlip's tunnel carries no traffic, so as the Always-on VPN with "Block
     * connections without VPN" it cuts the whole phone off the internet. Android
     * starts us with the plain VpnService action in that case. Refuse to act as
     * one and send the user straight to the setting.
     */
    private fun warnIfAlwaysOn() {
        if (!isAlwaysOn) return
        Log.w(TAG, "set as Always-on VPN (lockdown=$isLockdownEnabled)")
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(WARN_CHANNEL, getString(R.string.warn_channel), NotificationManager.IMPORTANCE_HIGH)
        )
        val fix = PendingIntent.getActivity(
            this, 4, Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = getString(if (isLockdownEnabled) R.string.always_on_lockdown else R.string.always_on)
        nm.notify(
            WARN_ID,
            Notification.Builder(this, WARN_CHANNEL)
                .setSmallIcon(R.drawable.ic_flip)
                .setContentTitle(getString(R.string.always_on_title))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(fix)
                .setAutoCancel(true)
                .build()
        )
        toast(getString(R.string.always_on_title))
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onRevoke() {
        down()
        super.onRevoke()
    }

    override fun onDestroy() {
        down()
        super.onDestroy()
    }
}
