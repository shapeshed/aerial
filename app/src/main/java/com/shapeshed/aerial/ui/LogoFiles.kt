package com.shapeshed.aerial.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import com.shapeshed.aerial.R
import android.webkit.MimeTypeMap
import androidx.core.graphics.createBitmap
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
import java.util.UUID

suspend fun copyLogoFromUri(context: Context, uri: Uri, directory: File): File? {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    val extension = extensionFromMimeType(mimeType)
        ?: uri.lastPathSegment?.extensionOrNull()
        ?: "img"
    val source = contentResolver.openInputStream(uri) ?: return null
    return source.use { input ->
        val dest = File(directory, "${UUID.randomUUID()}.$extension")
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

    return File(directory, "${UUID.randomUUID()}.$extension")
}

@Volatile private var svgLoader: ImageLoader? = null

private fun svgLoader(context: Context): ImageLoader =
    svgLoader ?: ImageLoader.Builder(context.applicationContext)
        .components { add(SvgDecoder.Factory()) }
        .build()
        .also { svgLoader = it }

suspend fun ensureMediaArtworkForLogo(context: Context, file: File): File {
    if (file.extension.lowercase(Locale.US) != "svg") return file

    val pngFile = mediaArtworkFile(file)
    if (pngFile.exists()) return pngFile

    return try {
        val request = ImageRequest.Builder(context)
            .data(file)
            .size(512)
            .build()
        val result = svgLoader(context).execute(request) as? SuccessResult ?: return file
        val bitmap = result.image.toBitmap() ?: return file
        pngFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        pngFile
    } catch (_: Exception) {
        file
    }
}

fun appIconBitmap(context: Context): ByteArray? {
    return try {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.aerial_icon_artwork)
            ?: return null
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        output.toByteArray()
    } catch (_: Exception) {
        null
    }
}

fun compressedLogoData(file: File): ByteArray? {
    val artworkFile = if (file.extension.lowercase(Locale.US) == "svg") {
        mediaArtworkFile(file).takeIf { it.exists() } ?: return null
    } else {
        file
    }
    if (!artworkFile.exists()) return null
    return when (artworkFile.extension.lowercase(Locale.US)) {
        "png", "jpg", "jpeg", "webp" -> artworkFile.readBytes()
        else -> {
            // Unknown extension (e.g. .img) — try BitmapFactory and re-encode as JPEG
            val bitmap = BitmapFactory.decodeFile(artworkFile.absolutePath) ?: return null
            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            bitmap.recycle()
            out.toByteArray()
        }
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
            createBitmap(width, height).also { bitmap ->
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        }
        else -> null
    }
}
