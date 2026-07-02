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
)
