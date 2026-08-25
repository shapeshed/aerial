package com.shapeshed.aerial.ui

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import java.io.File
import java.util.concurrent.CancellationException

/** Downloads and persists artwork through the app's configured image-loading pipeline. */
interface ArtworkLoader {
    suspend fun download(url: String, directory: File): String?
}

class CoilArtworkLoader(private val context: Context) : ArtworkLoader {
    override suspend fun download(url: String, directory: File): String? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(512)
                .build()
            val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult ?: return null
            val destination = logoFileForUrl(url, directory, "image/png")
            val bitmap = result.image.toOpaqueBitmap(context)
            val encoded = destination.outputStream().use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            }
            if (!encoded) {
                return null
            }
            destination.absolutePath
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }
}
