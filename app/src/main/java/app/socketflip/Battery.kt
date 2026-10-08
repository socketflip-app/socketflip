package app.socketflip

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Battery optimisation is the usual reason the floating button vanishes on phones
 * other than Pixels: the maker's battery saver closes SocketFlip's service in the
 * background. See https://dontkillmyapp.com for the per-brand details.
 */
object Battery {

    /** Brands known to close foreground services unless the app is exempted. */
    private val AGGRESSIVE = setOf(
        "samsung", "xiaomi", "redmi", "poco", "oneplus", "oppo", "realme", "vivo", "huawei", "honor", "meizu", "asus",
    )

    val aggressiveMaker: Boolean get() = Build.MANUFACTURER.lowercase() in AGGRESSIVE

    /**
     * Xiaomi, Redmi and POCO (MIUI, HyperOS) add their own "Open new windows while
     * running in the background" permission, off for sideloaded apps. Without it the
     * emergency restart is dropped silently, and Android gives no way to read it.
     */
    val xiaomi: Boolean get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    /** MIUI's "Other permissions" page for this app, else Android's App info. */
    fun openXiaomiPermissions(activity: Activity) {
        val pkg = activity.packageName
        val tries = listOf(
            Intent("miui.intent.action.APP_PERM_EDITOR")
                .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                .putExtra("extra_pkgname", pkg),
            Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", pkg),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")),
        )
        for (intent in tries) {
            try {
                activity.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // Not this phone's page; try the next.
            } catch (e: SecurityException) {
                // Some MIUI versions do not let other apps open it.
            }
        }
    }

    fun unrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Android's own "let this app run in the background?" prompt, or the settings list if the phone has none. */
    fun request(activity: Activity) {
        @Suppress("BatteryLife") // Not on Play; keeping the button alive is the whole point.
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${activity.packageName}"))
        try {
            activity.startActivity(direct)
        } catch (e: ActivityNotFoundException) {
            try {
                activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                // Nothing to open; the self-check screen still shows the state.
            }
        }
    }
}
