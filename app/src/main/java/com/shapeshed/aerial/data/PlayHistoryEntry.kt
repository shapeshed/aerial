package com.shapeshed.aerial.data

import androidx.room.Entity

/**
 * A station play, recorded regardless of favourite status — unlike [Station], where a row only
 * exists once a station is favourited. Powers "Recently Played" (Android Auto today; any surface
 * later) without duplicating station data: display info (name, logo, stream URL) is looked up
 * from the registry by provider+providerId at render time, not stored here. Only resolves for
 * registry-backed stations — locally-added custom stations have no provider identity and are
 * tracked separately via [Station.lastPlayedAt].
 */
@Entity(tableName = "play_history", primaryKeys = ["provider", "providerId"])
data class PlayHistoryEntry(
    val provider: String,
    val providerId: String,
    val playedAt: Long,
)
