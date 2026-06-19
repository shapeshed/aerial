package com.shapeshed.aerial.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.webkit.MimeTypeMap
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.svg.SvgDecoder
import java.io.File
import java.net.URL
import java.util.Locale

suspend fun copyLogoFromUri(context: Context, uri: Uri, directory: File): File? {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    val extension = extensionFromMimeType(mimeType)
        ?: uri.lastPathSegment?.extensionOrNull()
        ?: "img"
    val source = contentResolver.openInputStream(uri) ?: return null
    return source.use { input ->
        val dest = File(directory, "logo_${System.currentTimeMillis()}.$extension")
        dest.outputStream().use { output -> input.copyTo(output) }
        ensureMediaArtworkForLogo(context, dest)
        dest
    }
}

fun logoFileForUrl(url: String, directory: File, contentType: String?): File {
    val mimeType = contentType?.substringBefore(';')?.trim()
    val extension = extensionFromMimeType(mimeType)
        ?: URL(url).path.extensionOrNull()
        ?: "img"

    return File(directory, "logo_${System.currentTimeMillis()}.$extension")
}

suspend fun ensureMediaArtworkForLogo(context: Context, file: File): File {
    if (file.extension.lowercase(Locale.US) != "svg") return file

    val pngFile = mediaArtworkFile(file)
    if (pngFile.exists()) return pngFile

    val imageLoader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .build()
    return try {
        val request = ImageRequest.Builder(context)
            .data(file)
            .size(512)
            .build()
        val result = imageLoader.execute(request) as? SuccessResult ?: return file
        val bitmap = result.image.toBitmap() ?: return file
        pngFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        pngFile
    } catch (_: Exception) {
        file
    } finally {
        imageLoader.shutdown()
    }
}

fun compressedLogoData(file: File): ByteArray? {
    val artworkFile = mediaArtworkFile(file).takeIf { file.extension.lowercase(Locale.US) == "svg" } ?: file
    if (!artworkFile.exists()) return null
    return when (artworkFile.extension.lowercase(Locale.US)) {
        "png", "jpg", "jpeg", "webp" -> artworkFile.readBytes()
        else -> null
    }
}

private fun mediaArtworkFile(file: File): File {
    return if (file.extension.lowercase(Locale.US) == "svg") {
        File(file.parentFile, "${file.nameWithoutExtension}_media.png")
    } else {
        file
    }
}

private fun extensionFromMimeType(mimeType: String?): String? {
    return mimeType?.let {
        MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(it)
            ?.lowercase(Locale.US)
    }
}

private fun String.extensionOrNull(): String? {
    return substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
        .takeIf { it.isNotBlank() && it.length <= 5 }
}

private fun Image.toBitmap(): Bitmap? {
    return when (this) {
        is BitmapImage -> bitmap
        is DrawableImage -> {
            val width = width.takeIf { it > 0 } ?: 512
            val height = height.takeIf { it > 0 } ?: 512
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }
        else -> null
    }
}
