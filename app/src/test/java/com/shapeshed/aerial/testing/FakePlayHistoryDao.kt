package com.shapeshed.aerial.testing

import com.shapeshed.aerial.data.PlayHistoryDao
import com.shapeshed.aerial.data.PlayHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** In-memory [PlayHistoryDao] shared by the repository and browse-tree tests. */
internal class FakePlayHistoryDao(vararg initialEntries: PlayHistoryEntry) : PlayHistoryDao {
    val entries = initialEntries.toMutableList()

    override suspend fun recordPlay(entry: PlayHistoryEntry) {
        entries.removeAll { it.provider == entry.provider && it.providerId == entry.providerId }
        entries += entry
    }

    override suspend fun recent(limit: Int): List<PlayHistoryEntry> =
        entries.sortedByDescending { it.playedAt }.take(limit)

    override fun recentAsFlow(limit: Int): Flow<List<PlayHistoryEntry>> =
        flowOf(entries.sortedByDescending { it.playedAt }.take(limit))
}
