package com.shapeshed.aerial.data

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame

/**
 * The stream metadata frames the now-playing display cares about, pulled out of a
 * media3 [Metadata] bundle: the ICY title, ID3 title/artist, and ID3 artwork bytes.
 */
class StreamMetadataFrames(
    val icyTitle: String?,
    val id3Title: String?,
    val id3Artist: String?,
    val id3Artwork: ByteArray?,
)

/** Metadata values that changed since the previous update, with each source decided independently. */
data class StreamMetadataChanges(
    val icyTitle: String?,
    val id3Title: String?,
    val id3Artist: String?,
    val id3Artwork: ByteArray?,
)

fun streamMetadataChanges(
    frames: StreamMetadataFrames,
    lastIcyTitle: String?,
    lastId3Title: String?,
): StreamMetadataChanges {
    val icyTitle = frames.icyTitle
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != lastIcyTitle }
    val id3Changed = frames.id3Title?.takeIf { it.isNotBlank() && it != lastId3Title }
    return StreamMetadataChanges(
        icyTitle = icyTitle,
        id3Title = id3Changed,
        id3Artist = id3Changed?.let { frames.id3Artist },
        id3Artwork = id3Changed?.let { frames.id3Artwork },
    )
}

@OptIn(UnstableApi::class)
fun streamMetadataFrames(metadata: Metadata): StreamMetadataFrames {
    var icyTitle: String? = null
    var id3Title: String? = null
    var id3Artist: String? = null
    var id3Artwork: ByteArray? = null

    for (index in 0 until metadata.length()) {
        when (val entry = metadata[index]) {
            is IcyInfo -> icyTitle = entry.title

            is TextInformationFrame -> when (entry.id) {
                "TIT2" -> id3Title = entry.values.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                "TPE1" -> id3Artist = entry.values.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
            }

            is ApicFrame -> id3Artwork = entry.pictureData

            else -> Unit
        }
    }
    return StreamMetadataFrames(icyTitle, id3Title, id3Artist, id3Artwork)
}
