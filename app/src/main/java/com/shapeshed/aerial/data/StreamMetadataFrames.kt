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
