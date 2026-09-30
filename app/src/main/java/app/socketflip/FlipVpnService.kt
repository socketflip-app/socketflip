package app.socketflip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArraySet

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

    private var netCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        const val ACTION_FLIP = "app.socketflip.action.FLIP"
        const val ACTION_STOP = "app.socketflip.action.STOP"
        const val ACTION_CHECK = "app.socketflip.action.CHECK"

        private const val TAG = "SocketFlip"
        // A private /32 pair the target app will never talk to, so nothing is routed.
        private const val TUN_ADDRESS = "10.111.222.1"
        private const val TUN_ROUTE = "10.111.222.2"
        private const val WARN_CHANNEL = "warnings"
        private const val WARN_ID = 2
        private val FALLBACK_DNS = listOf("1.1.1.1", "9.9.9.9")

        @Volatile private var tun: ParcelFileDescriptor? = null
        @Volatile private var lastFlip = 0L

        val isUp: Boolean get() = tun != null

        /**
         * Same resolvers, ignoring order: a DHCP renew often lists the same servers
         * in a different order, and that must not rebuild the tunnel (a rebuild is
         * an extra disconnect for the target app). Pure, so the unit tests cover it.
         */
        fun sameDns(a: List<InetAddress>, b: List<InetAddress>): Boolean =
            a.map { it.hostAddress }.toSet() == b.map { it.hostAddress }.toSet()

        /** Called on the main thread whenever the tunnel goes up or down. */
        val listeners = CopyOnWriteArraySet<() -> Unit>()

        /**
         * How much of the cooldown is left, from 1 (just flipped) to 0 (ready).
         * A second disconnect while the target is still reconnecting can leave it
         * stuck on its reconnect screen, so flips are spaced at least this far apart.
         */
        fun cooldownLeft(context: Context): Float {
            if (lastFlip == 0L) return 0f
            val total = Prefs.cooldownMs(context)
            val left = lastFlip + total - SystemClock.elapsedRealtime()
            return if (left <= 0) 0f else left.toFloat() / total
        }

        /**
         * True when a VPN other than SocketFlip is carrying this phone's traffic.
         * SocketFlip's own tunnel never covers SocketFlip itself, so any VPN seen
         * here belongs to another app, and bringing ours up will replace it.
         */
        // Looks at every network, not just activeNetwork: a per-app VPN (one that only covers a
        // game, say) is not SocketFlip's own default network, so activeNetwork never shows it.
        // While our tunnel is up, the VPN that is there is ours.
        fun otherVpnActive(context: Context): Boolean = try {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            @Suppress("DEPRECATION")
            tun == null && cm != null && cm.allNetworks.any { n ->
                cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
        } catch (e: SecurityException) {
            false
        }

        fun flip(context: Context) = send(context, ACTION_FLIP)

        fun stop(context: Context) = send(context, ACTION_STOP)

        /** Warns if SocketFlip has been made the Always-on VPN (see [warnIfAlwaysOn]). */
        fun check(context: Context) = send(context, ACTION_CHECK)

        private fun send(context: Context, action: String) {
            try {
                context.startService(Intent(context, FlipVpnService::class.java).setAction(action))
            } catch (e: IllegalStateException) {
                // Background start refused: say so, or a tile tap would do nothing at all.
                Log.w(TAG, "could not start service for $action", e)
                Toast.makeText(context, context.getString(R.string.start_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Never crash: a crash dialog would land on top of the target app.
        try {
            when (intent?.action) {
                ACTION_FLIP -> flipOnce()
                ACTION_STOP -> stopTunnel()
            }
            warnIfAlwaysOn()
        } catch (e: Exception) {
            Log.e(TAG, "flip failed", e)
            toast(getString(R.string.flip_failed, e.message ?: e.javaClass.simpleName))
        }
        refreshTile()
        // Only stop if no newer command is queued: a FLIP right behind this one may
        // have just brought the tunnel up, and stopping would take it down again.
        if (tun == null) stopSelfResult(startId)
        return START_NOT_STICKY
    }

    private fun flipOnce() {
        val now = SystemClock.elapsedRealtime()
        if (cooldownLeft(this) > 0f) {
            if (Prefs.hints(this)) toast(getString(R.string.cooldown))
            return
        }
        val wasUp = tun != null
        if (wasUp) down() else {
            val replacing = otherVpnActive(this)
            up()
            if (replacing && tun != null && Prefs.hints(this)) toast(getString(R.string.replaced_vpn))
        }
        // Only a real state change disconnected the app; a flip that bailed must not
        // hold the next tap back behind the cooldown.
        if ((tun != null) != wasUp) {
            lastFlip = now
            Prefs.countFlip(this)
        }
    }

    /**
     * Taking the tunnel down closes the target's connections just like a flip, so it
     * starts the cooldown too: the next tap then waits instead of hitting the target
     * a second time while it is still reconnecting.
     */
    private fun stopTunnel() {
        if (tun == null) return
        down()
        lastFlip = SystemClock.elapsedRealtime()
    }

    private fun up() {
        // Check the targets first: prepare() below is not a harmless question, it takes
        // the VPN slot from any other VPN app, so never call it for a flip that cannot work.
        val targets = Prefs.checked(this).filter { installed(it) }
        if (targets.isEmpty()) {
            toast(getString(if (Prefs.checked(this).isEmpty()) R.string.target_unset else R.string.target_missing))
            return
        }
        if (prepare(this) != null) {
            // Permission was there before, so something took it: almost always another
            // VPN app set as Always-on, which Android will not let SocketFlip replace.
            toast(getString(if (Prefs.flips(this) > 0) R.string.vpn_taken else R.string.need_vpn))
            return
        }
        val dns = dnsServers()
        tun = establish(targets, dns)
        Log.i(TAG, "tunnel up for $targets: ${tun != null}")
        // null means the system refused, most often because another app holds Always-on.
        if (tun == null) toast(getString(R.string.tunnel_failed)) else watchNetwork(targets, dns)
    }

    private fun installed(pkg: String): Boolean = try {
        packageManager.getApplicationInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** One tunnel covering every ticked app, so one state change reconnects them all. */
    private fun establish(targets: List<String>, dns: List<InetAddress>): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(TUN_ADDRESS, 32)
            .addRoute(TUN_ROUTE, 32)
            .setMtu(1280)
            .setBlocking(false)
            .setMetered(false)
        targets.forEach { builder.addAllowedApplication(it) }
        dns.forEach { builder.addDnsServer(it) }
        return builder.establish()
    }

    /**
     * The tunnel copies the network's DNS servers when it comes up. If the phone
     * then moves network (home Wi-Fi to mobile data, say) those servers can be out
     * of reach, typically a router or LAN resolver, and every lookup the target app
     * makes fails while the tunnel is up. So while it is up, follow the default
     * network and rebuild the tunnel with the new servers when they change.
     *
     * The network change has already dropped the target app's connections, and its
     * reconnect cannot succeed until DNS works, so the rebuild costs no extra
     * disconnect that matters.
     */
    private fun watchNetwork(targets: List<String>, initial: List<InetAddress>) {
        unwatchNetwork()
        var current = initial
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) {
                if (tun == null) return
                val fresh = usableDns(lp.dnsServers).ifEmpty { fallbackDns() }
                if (sameDns(fresh, current)) return
                Log.i(TAG, "network DNS changed, rebuilding tunnel")
                try {
                    val old = tun
                    val replacement = establish(targets, fresh) ?: return
                    tun = replacement
                    current = fresh
                    try {
                        old?.close()
                    } catch (e: Exception) {
                        Log.w(TAG, "close of old tunnel failed", e)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "tunnel rebuild failed", e)
                }
            }
        }
        try {
            getSystemService(ConnectivityManager::class.java)
                ?.registerDefaultNetworkCallback(cb, Handler(Looper.getMainLooper()))
            netCallback = cb
        } catch (e: Exception) {
            Log.w(TAG, "could not watch the network", e)
        }
    }

    private fun unwatchNetwork() {
        netCallback?.let {
            try {
                getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.w(TAG, "unregister failed", e)
            }
        }
        netCallback = null
    }

    private fun down() {
        unwatchNetwork()
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
            usableDns(cm?.activeNetwork?.let { cm.getLinkProperties(it)?.dnsServers }.orEmpty())
        } catch (e: SecurityException) {
            emptyList()
        }
        return current.ifEmpty { fallbackDns() }
    }

    private fun usableDns(servers: List<InetAddress>) =
        servers.filter { it is Inet4Address && !it.isLinkLocalAddress }

    private fun fallbackDns() = FALLBACK_DNS.map { InetAddress.getByName(it) }

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

    /** Tells the tile, and anything else watching, that the tunnel may have changed. */
    private fun refreshTile() {
        listeners.forEach { it() }
        try {
            TileService.requestListeningState(this, ComponentName(this, FlipTileService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "tile refresh failed", e)
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onRevoke() {
        down()
        refreshTile()
        super.onRevoke()
    }

    override fun onDestroy() {
        down()
        super.onDestroy()
    }
}
