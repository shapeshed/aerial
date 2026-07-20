package com.shapeshed.aerial.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class Station(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val streamUrl: String,
    val logoPath: String = "",
    val isFavorite: Boolean = false,
    val provider: String = "",
    val providerId: String = "",
    val tags: String = "",
    val description: String = "",
    val country: String = "",
    val countryCode: String = "",
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0,
) {
    /** Same station by identity, not by value: a saved row and an ephemeral/registry copy of
     * it (id 0, no db row) should still match by provider+providerId or stream URL. */
    fun matches(other: Station): Boolean =
        (id != 0L && id == other.id) ||
            streamUrl == other.streamUrl ||
            (provider.isNotBlank() &&
                providerId.isNotBlank() &&
                provider == other.provider &&
                providerId == other.providerId)
}

/** Resolves where [target] sits in [queue] for queueing playback — null if it isn't a member,
 * or the queue is a single station (nothing to skip to). */
fun resolveQueueStart(queue: List<Station>, target: Station): Int? =
    queue.indexOfFirst { it.matches(target) }.takeIf { it >= 0 && queue.size > 1 }
