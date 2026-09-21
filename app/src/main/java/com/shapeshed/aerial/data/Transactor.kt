package com.shapeshed.aerial.data

import androidx.room.withTransaction

/**
 * Runs a repository read-modify-write sequence atomically. Production wires Room's
 * `withTransaction`; the default runs inline so JVM tests need no database.
 */
interface Transactor {
    suspend fun <R> run(block: suspend () -> R): R
}

/** Runs [block] directly. Used by tests, and as the default for a repository. */
object InlineTransactor : Transactor {
    override suspend fun <R> run(block: suspend () -> R): R = block()
}

/** Runs [block] inside a Room database transaction. */
class RoomTransactor(private val database: StationDatabase) : Transactor {
    override suspend fun <R> run(block: suspend () -> R): R = database.withTransaction { block() }
}
