package app.socketflip

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Hands out one file, read-only: SocketFlip's own installed APK, so "Send the app"
 * can pass it to a chat or Quick Share. It is the same signed file as the GitHub
 * release, so a friend who installs it still gets updates from there.
 *
 * Not exported: another app can read it only through the one-off permission that
 * comes with the share. Whatever the path in the address, it can only ever return
 * this APK, and nothing can be written. (A tiny provider of our own instead of
 * AndroidX's FileProvider, since the app has no AndroidX.)
 */
class ApkProvider : ContentProvider() {

    companion object {
        const val MIME = "application/vnd.android.package-archive"

        fun uri(context: Context): Uri = Uri.parse("content://${context.packageName}.apk/${fileName(context)}")

        /** The name the receiving app shows, e.g. socketflip-1.13.apk, as on the releases page. */
        fun fileName(context: Context): String {
            val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "app"
            return "socketflip-$version.apk"
        }

        /** Only a single, whole APK can be sent; an install split into parts cannot. */
        fun available(context: Context): Boolean =
            context.applicationInfo.splitSourceDirs.isNullOrEmpty() && apk(context).canRead()

        private fun apk(context: Context) = File(context.applicationInfo.sourceDir)
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = MIME

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("read only")
        val context = context ?: throw FileNotFoundException()
        return ParcelFileDescriptor.open(apk(context), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    /** Name and size, which chat apps ask for before they take the file. */
    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val context = context ?: return MatrixCursor(arrayOf())
        val columns = (projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
            .filter { it == OpenableColumns.DISPLAY_NAME || it == OpenableColumns.SIZE }
            .toTypedArray()
        return MatrixCursor(columns, 1).apply {
            addRow(columns.map { if (it == OpenableColumns.SIZE) apk(context).length() else fileName(context) })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
}
