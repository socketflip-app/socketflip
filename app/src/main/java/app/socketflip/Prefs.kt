package app.socketflip

import android.content.Context
import android.content.pm.PackageManager

object Prefs {
    private const val FILE = "socketflip"
    private const val KEY_TARGET = "target"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"

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
}
