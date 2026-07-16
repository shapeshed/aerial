package com.shapeshed.aerial

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * Read-only provider serving rasterized registry logos (SVG -> PNG, see
 * [com.shapeshed.aerial.ui.cachedRegistryArtworkUri]) to external artwork consumers — Android
 * Auto's browse lists, Bluetooth AVRCP, System UI. A stable content:// artworkUri lets those
 * consumers decode once and cache by URI (embedded artworkData bytes can't be cached that way,
 * which shows up as icons flashing in on every list render). Exported because media artwork
 * URIs offer no permission-grant handshake; it can only ever open files inside the
 * registry_artwork cache directory.
 */
class ArtworkProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val context = context ?: throw FileNotFoundException(uri.toString())
        val fileName = uri.lastPathSegment ?: throw FileNotFoundException(uri.toString())
        val dir = File(context.cacheDir, ARTWORK_CACHE_DIR)
        val file = File(dir, fileName)
        if (file.canonicalFile.parentFile != dir.canonicalFile || !file.exists()) {
            throw FileNotFoundException(uri.toString())
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "image/png"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        const val ARTWORK_CACHE_DIR = "registry_artwork"

        fun uriFor(context: android.content.Context, fileName: String): Uri =
            Uri.parse("content://${context.packageName}.artwork/$fileName")
    }
}
