package app.socketflip

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

object Prefs {
    private const val FILE = "socketflip"
    private const val KEY_TARGET = "target"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"
    private const val KEY_FLIPS = "flips"
    private const val KEY_TIP_DISMISSED = "tip_dismissed"
    private const val KEY_OTHER_VPN_WARNED = "other_vpn_warned"
    private const val KEY_BATTERY_ASKED = "battery_asked"
    private const val KEY_COOLDOWN = "cooldown_s"
    private const val KEY_HAPTICS = "haptics"
    private const val KEY_HINTS = "hints"
    private const val KEY_SNAP = "snap_to_edge"
    private const val KEY_ONLY_OVER_TARGET = "only_over_target"
    private const val KEY_DROP_ON_LEAVE = "drop_on_leave"
    private const val KEY_TOUCH = "touch"
    private const val KEY_DOWN = "look_down"
    private const val KEY_UP = "look_up"
    private const val KEY_RING = "look_ring"
    private const val KEY_ICON = "look_icon"
    private const val KEY_IDLE_ALPHA = "look_idle_alpha"
    private const val KEY_PRESSED_ALPHA = "look_pressed_alpha"
    private const val KEY_SIZE = "look_size"

    const val COOLDOWN_MIN_S = 3
    const val COOLDOWN_MAX_S = 60
    const val COOLDOWN_DEFAULT_S = 10

    /** Where the tip button goes: a Stripe pay-what-you-want page, opened in the browser. */
    const val TIP_URL = "https://buy.stripe.com/cNidR84Lg4pT36l35jfnO00"

    /** The same support, for people who prefer Ko-fi. */
    const val KOFI_URL = "https://ko-fi.com/socketflip"

    /** Latest release page; the browser shows the version and the download. */
    const val RELEASES_URL = "https://github.com/socketflip-app/socketflip/releases/latest"

    /** Source code, as the GPL expects users to be able to find it. */
    const val SOURCE_URL = "https://github.com/socketflip-app/socketflip"

    /** The page that explains, for the wary, what the VPN permission is and is not used for. */
    const val SAFETY_URL = "$SOURCE_URL/blob/main/docs/SAFETY.md"

    /** Successful flips before the one-time "enjoying it?" card appears. */
    const val TIP_PROMPT_AFTER = 25

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun target(context: Context): String? = prefs(context).getString(KEY_TARGET, null)

    fun setTarget(context: Context, pkg: String) =
        prefs(context).edit().putString(KEY_TARGET, pkg).apply()

    fun label(context: Context, pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    fun position(context: Context): Pair<Int, Int> =
        prefs(context).let { it.getInt(KEY_X, 0) to it.getInt(KEY_Y, 400) }

    fun setPosition(context: Context, x: Int, y: Int) =
        prefs(context).edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply()

    fun flips(context: Context): Int = prefs(context).getInt(KEY_FLIPS, 0)

    fun countFlip(context: Context) =
        prefs(context).edit().putInt(KEY_FLIPS, flips(context) + 1).apply()

    fun tipDismissed(context: Context): Boolean = prefs(context).getBoolean(KEY_TIP_DISMISSED, false)

    fun dismissTip(context: Context) =
        prefs(context).edit().putBoolean(KEY_TIP_DISMISSED, true).apply()

    fun otherVpnWarned(context: Context): Boolean = prefs(context).getBoolean(KEY_OTHER_VPN_WARNED, false)

    fun setOtherVpnWarned(context: Context) =
        prefs(context).edit().putBoolean(KEY_OTHER_VPN_WARNED, true).apply()

    fun batteryAsked(context: Context): Boolean = prefs(context).getBoolean(KEY_BATTERY_ASKED, false)

    fun setBatteryAsked(context: Context) =
        prefs(context).edit().putBoolean(KEY_BATTERY_ASKED, true).apply()

    fun cooldownSeconds(context: Context): Int =
        prefs(context).getInt(KEY_COOLDOWN, COOLDOWN_DEFAULT_S).coerceIn(COOLDOWN_MIN_S, COOLDOWN_MAX_S)

    fun cooldownMs(context: Context): Long = cooldownSeconds(context) * 1000L

    fun setCooldownSeconds(context: Context, s: Int) =
        prefs(context).edit().putInt(KEY_COOLDOWN, s.coerceIn(COOLDOWN_MIN_S, COOLDOWN_MAX_S)).apply()

    /** Vibrate on a tap of the floating button. */
    fun haptics(context: Context): Boolean = prefs(context).getBoolean(KEY_HAPTICS, true)

    fun setHaptics(context: Context, on: Boolean) = prefs(context).edit().putBoolean(KEY_HAPTICS, on).apply()

    /** Informational toasts ("still reconnecting"). Errors are always shown. */
    fun hints(context: Context): Boolean = prefs(context).getBoolean(KEY_HINTS, true)

    fun setHints(context: Context, on: Boolean) = prefs(context).edit().putBoolean(KEY_HINTS, on).apply()

    /** Slide the button to the nearest side of the screen when it is let go. */
    fun snapToEdge(context: Context): Boolean = prefs(context).getBoolean(KEY_SNAP, false)

    fun setSnapToEdge(context: Context, on: Boolean) = prefs(context).edit().putBoolean(KEY_SNAP, on).apply()

    fun look(context: Context): Look {
        val p = prefs(context)
        val d = Look.DEFAULT
        return Look(
            downColor = p.getInt(KEY_DOWN, d.downColor),
            upColor = p.getInt(KEY_UP, d.upColor),
            ringColor = p.getInt(KEY_RING, d.ringColor),
            iconColor = p.getInt(KEY_ICON, d.iconColor),
            idleAlpha = p.getFloat(KEY_IDLE_ALPHA, d.idleAlpha).coerceIn(Look.MIN_ALPHA, 1f),
            pressedAlpha = p.getFloat(KEY_PRESSED_ALPHA, d.pressedAlpha).coerceIn(Look.MIN_ALPHA, 1f),
            sizeDp = p.getInt(KEY_SIZE, d.sizeDp).coerceIn(Look.MIN_SIZE_DP, Look.MAX_SIZE_DP),
        )
    }

    fun setLook(context: Context, look: Look) = prefs(context).edit()
        .putInt(KEY_DOWN, look.downColor)
        .putInt(KEY_UP, look.upColor)
        .putInt(KEY_RING, look.ringColor)
        .putInt(KEY_ICON, look.iconColor)
        .putFloat(KEY_IDLE_ALPHA, look.idleAlpha.coerceIn(Look.MIN_ALPHA, 1f))
        .putFloat(KEY_PRESSED_ALPHA, look.pressedAlpha.coerceIn(Look.MIN_ALPHA, 1f))
        .putInt(KEY_SIZE, look.sizeDp.coerceIn(Look.MIN_SIZE_DP, Look.MAX_SIZE_DP))
        .apply()

    /** Lets the floating button redraw as soon as a setting changes. */
    fun listen(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).registerOnSharedPreferenceChangeListener(listener)

    fun unlisten(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)

    /** Hide the floating button unless the target app is on screen (needs Usage Access). */
    fun onlyOverTarget(context: Context): Boolean = prefs(context).getBoolean(KEY_ONLY_OVER_TARGET, false)

    fun setOnlyOverTarget(context: Context, on: Boolean) =
        prefs(context).edit().putBoolean(KEY_ONLY_OVER_TARGET, on).apply()

    /** Take the tunnel down when the user leaves the target app (needs Usage Access). */
    fun dropOnLeave(context: Context): Boolean = prefs(context).getBoolean(KEY_DROP_ON_LEAVE, false)

    fun setDropOnLeave(context: Context, on: Boolean) =
        prefs(context).edit().putBoolean(KEY_DROP_ON_LEAVE, on).apply()

    /** Fires the change listeners without changing a real setting (after a permission comes back). */
    fun touch(context: Context) = prefs(context).edit().putLong(KEY_TOUCH, System.nanoTime()).apply()
}
