package app.socketflip

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Icon
import android.widget.Toast

/**
 * A home screen icon that shows the floating button and opens one app, so a game
 * starts with SocketFlip ready in a single tap. The icon is the app's own at full
 * size; launchers mark pinned shortcuts with the publishing app's icon, which is what
 * tells it apart from the original (a badge of our own showed up as a second one).
 */
object GameShortcut {

    fun pin(context: Context, pkg: String) {
        val sm = context.getSystemService(ShortcutManager::class.java)
        if (sm == null || !sm.isRequestPinShortcutSupported) {
            Toast.makeText(context, R.string.shortcut_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        val name = Prefs.label(context, pkg)
        val intent = Intent(ShortcutActivity.ACTION_LAUNCH)
            .setClassName(context.packageName, ShortcutActivity::class.java.name)
            .putExtra(ShortcutActivity.EXTRA_PACKAGE, pkg)
        val info = ShortcutInfo.Builder(context, "launch:$pkg")
            .setShortLabel(name)
            .setLongLabel(context.getString(R.string.shortcut_long, name))
            .setIcon(Icon.createWithAdaptiveBitmap(appIcon(context, pkg)))
            .setIntent(intent)
            .build()
        // Android shows its own "Add to home screen?" confirmation.
        sm.requestPinShortcut(info, null)
    }

    /**
     * An adaptive icon bitmap (108 dp, the launcher masks it to its own shape and shows
     * the middle 72 dp), so the app's art fills the icon like a normal app icon instead
     * of being shrunk onto a plate.
     */
    private fun appIcon(context: Context, pkg: String): Bitmap {
        val size = (108 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val app = try {
            context.packageManager.getApplicationIcon(pkg)
        } catch (e: Exception) {
            context.getDrawable(R.drawable.ic_launcher)!!
        }
        if (app is AdaptiveIconDrawable) {
            // Both layers are already drawn for the full 108 dp canvas.
            app.background?.let { it.setBounds(0, 0, size, size); it.draw(canvas) }
            app.foreground?.let { it.setBounds(0, 0, size, size); it.draw(canvas) }
        } else {
            // An old-style icon: a white backdrop, the icon scaled to the visible area.
            canvas.drawColor(0xFFFFFFFF.toInt())
            val inset = size / 6
            app.setBounds(inset, inset, size - inset, size - inset)
            app.draw(canvas)
        }

        return bitmap
    }
}
