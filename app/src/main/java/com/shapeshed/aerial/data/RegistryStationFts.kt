package com.shapeshed.aerial.data

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Full-text search index over [RegistryStation]. External-content FTS4 (no duplicated storage;
 * reads columns straight from registry_stations, kept in sync by rebuilding after each import).
 *
 * The unicode61 tokenizer with remove_diacritics=1 folds accents on both the indexed text and
 * the query, so "cafe" matches "Café" and vice-versa. remove_diacritics=2 would be better but
 * needs SQLite >= 3.27 (~API 30); =1 works on the app's minSdk 26 and covers Latin accents.
 */
@Fts4(
    contentEntity = RegistryStation::class,
    tokenizer = "unicode61",
    tokenizerArgs = ["remove_diacritics=1"],
)
@Entity(tableName = "registry_stations_fts")
data class RegistryStationFts(
    val searchText: String,
    val description: String,
    val country: String,
)
