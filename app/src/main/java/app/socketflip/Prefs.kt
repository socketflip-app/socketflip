package app.socketflip

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import java.security.SecureRandom

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
    private const val KEY_TARGETS = "targets"
    private const val KEY_CHECKED = "checked"
    private const val KEY_TARGET_COOLDOWN = "cooldown_s:"
    private const val KEY_AUTOMATION = "automation"
    private const val KEY_EMERGENCY = "emergency_restart"
    private const val KEY_AUTOMATION_TOKEN = "automation_token"
    private const val KEY_DOWN = "look_down"
    private const val KEY_UP = "look_up"
    private const val KEY_RING = "look_ring"
    private const val KEY_ICON = "look_icon"
    private const val KEY_IDLE_ALPHA = "look_idle_alpha"
    private const val KEY_PRESSED_ALPHA = "look_pressed_alpha"
    private const val KEY_SIZE = "look_size"
    private const val KEY_MIRROR = "look_mirror"

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
    const val SAFETY_WHY_VPN_URL = "$SAFETY_URL#why-a-vpn"
    const val NEW_ISSUE_URL = "$SOURCE_URL/issues/new"

    /** Successful flips before the one-time "enjoying it?" card appears. */
    const val TIP_PROMPT_AFTER = 25

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The active target: the app the next flip applies to. */
    /** Every app the user has added, in the order added. Versions before 1.10 kept only one. */
    fun targets(context: Context): List<String> {
        val stored = prefs(context).getString(KEY_TARGETS, null)?.split("\n")?.filter { it.isNotEmpty() }
        return stored ?: listOfNotNull(prefs(context).getString(KEY_TARGET, null))
    }

    /** The ticked apps: every tap reconnects all of these at once. */
    fun checked(context: Context): List<String> {
        val stored = prefs(context).getString(KEY_CHECKED, null)?.split("\n")?.filter { it.isNotEmpty() }
        val all = targets(context)
        return (stored ?: all).filter { it in all }
    }

    fun setChecked(context: Context, pkg: String, on: Boolean) {
        val now = checked(context).toMutableList()
        if (on && pkg !in now) now += pkg
        if (!on) now -= pkg
        prefs(context).edit().putString(KEY_CHECKED, now.joinToString("\n")).apply()
    }

    /** Adds [pkg] to the list, ticked. */
    fun addTarget(context: Context, pkg: String) {
        val list = targets(context)
        if (pkg in list) return setChecked(context, pkg, true)
        val ticked = checked(context) + pkg
        prefs(context).edit()
            .putString(KEY_TARGETS, (list + pkg).joinToString("\n"))
            .putString(KEY_CHECKED, ticked.joinToString("\n"))
            .apply()
    }

    fun removeTarget(context: Context, pkg: String) {
        val list = targets(context) - pkg
        val ticked = checked(context) - pkg
        prefs(context).edit()
            .putString(KEY_TARGETS, list.joinToString("\n"))
            .putString(KEY_CHECKED, ticked.joinToString("\n"))
            .remove(KEY_TARGET_COOLDOWN + pkg)
            .apply()
    }

    /** The ticked apps' names, for messages and the notification. */
    fun checkedLabels(context: Context): String = checked(context).joinToString(", ") { label(context, it) }

    /** A cooldown just for [pkg], or null to use the one from Settings. */
    fun targetCooldownSeconds(context: Context, pkg: String): Int? {
        val s = prefs(context).getInt(KEY_TARGET_COOLDOWN + pkg, 0)
        return if (s == 0) null else s.coerceIn(COOLDOWN_MIN_S, COOLDOWN_MAX_S)
    }

    fun setTargetCooldownSeconds(context: Context, pkg: String, s: Int?) {
        val edit = prefs(context).edit()
        if (s == null) edit.remove(KEY_TARGET_COOLDOWN + pkg)
        else edit.putInt(KEY_TARGET_COOLDOWN + pkg, s.coerceIn(COOLDOWN_MIN_S, COOLDOWN_MAX_S))
        edit.apply()
    }

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

    /**
     * The cooldown that applies right now: the longest among the ticked apps, each
     * using its own cooldown or the one from Settings. The slowest app to reconnect
     * sets the pace, since every tap reconnects all of them.
     */
    fun cooldownMs(context: Context): Long {
        val global = cooldownSeconds(context)
        val longest = checked(context).maxOfOrNull { targetCooldownSeconds(context, it) ?: global } ?: global
        return longest * 1000L
    }

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
            mirrorIcon = p.getBoolean(KEY_MIRROR, d.mirrorIcon),
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
        .putBoolean(KEY_MIRROR, look.mirrorIcon)
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

    /** Let other apps (Tasker, MacroDroid, Key Mapper) trigger a flip with a broadcast. */
    fun automation(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTOMATION, false)

    fun setAutomation(context: Context, on: Boolean) = prefs(context).edit().putBoolean(KEY_AUTOMATION, on).apply()

    /** During the cooldown the button becomes a red "!" that restarts the app (after asking). */
    fun emergencyRestart(context: Context): Boolean = prefs(context).getBoolean(KEY_EMERGENCY, false)

    fun setEmergencyRestart(context: Context, on: Boolean) =
        prefs(context).edit().putBoolean(KEY_EMERGENCY, on).apply()

    /**
     * A random code, made once per install, that an automation broadcast must carry
     * as the extra "token". Only the automation app the user pasted it into knows it.
     */
    fun automationToken(context: Context): String {
        prefs(context).getString(KEY_AUTOMATION_TOKEN, null)?.let { return it }
        val chars = "abcdefghijkmnpqrstuvwxyz23456789"
        val random = SecureRandom()
        val token = String(CharArray(16) { chars[random.nextInt(chars.length)] })
        prefs(context).edit().putString(KEY_AUTOMATION_TOKEN, token).apply()
        return token
    }
}
