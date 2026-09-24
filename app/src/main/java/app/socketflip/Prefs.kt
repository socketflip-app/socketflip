package app.socketflip

import android.content.Context
import android.content.pm.PackageManager

object Prefs {
    private const val FILE = "socketflip"
    private const val KEY_TARGET = "target"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"
    private const val KEY_FLIPS = "flips"
    private const val KEY_TIP_DISMISSED = "tip_dismissed"

    /** Where the tip button goes: a Stripe pay-what-you-want page, opened in the browser. */
    const val TIP_URL = "https://buy.stripe.com/cNidR84Lg4pT36l35jfnO00"

    /** Latest release page; the browser shows the version and the download. */
    const val RELEASES_URL = "https://github.com/socketflip-app/socketflip/releases/latest"

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
}
