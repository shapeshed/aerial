package com.shapeshed.aerial.data

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search index over locally saved stations. Local station rows are the source of truth
 * for favorites, including user-edited names, stream URLs, and registry identity.
 */
@Fts4(
    contentEntity = Station::class,
    tokenizer = "unicode61",
    tokenizerArgs = ["remove_diacritics=1"],
)
@Entity(tableName = "stations_fts")
data class StationFts(
    val name: String,
    val streamUrl: String,
    val provider: String,
    val providerId: String,
    val tags: String,
    val description: String,
    val country: String,
)
