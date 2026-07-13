package com.shapeshed.aerial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.core.graphics.createBitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers

// Media3's default BitmapLoader fetches artwork with its own bare HTTP stack and decodes it
// with BitmapFactory: it can't render SVG logos at all, and it doesn't send the identified
// User-Agent some hosts require (see AerialApp's Coil client) — so those stations show a
// logo in the in-app mini player (which goes through Coil) but not in the media notification,
// lock screen, quick settings player, or Android Auto. Routing artwork through the app's own
// Coil image loader gives every system surface the same SVG decoding and networking as the UI.
@UnstableApi
class CoilBitmapLoader(private val context: Context) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.Default, false) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: error("Could not decode artwork bytes")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        SuspendToFutureAdapter.launchFuture(Dispatchers.IO, false) {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(Size.ORIGINAL)
                .build()
            val result = imageLoader.execute(request) as? SuccessResult
                ?: error("Could not load artwork from $uri")
            when (val image = result.image) {
                // The common case: a decoded raster logo. Coil may hand back a hardware
                // bitmap for performance; system notifications need a software one, and
                // Bitmap.copy() does that GPU-to-software readback correctly — unlike
                // Canvas, which throws "Software rendering doesn't support hardware
                // bitmaps" if you try to draw a hardware bitmap into a software canvas.
                is BitmapImage -> if (image.bitmap.config == Bitmap.Config.HARDWARE) {
                    image.bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    image.bitmap
                }
                // Any other Image implementation (e.g. a vector drawable) isn't backed by a
                // hardware bitmap, so drawing it into a fresh software canvas is safe.
                else -> createBitmap(image.width, image.height).apply {
                    image.draw(Canvas(this))
                }
            }
        }
}
