package com.shapeshed.aerial.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registry_stations")
data class RegistryStation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val country: String = "",
    val countryCode: String = "",
    val tags: String = "",
    val provider: String = "",
    val providerId: String = "",
    val description: String = "",
    val searchText: String = "",
)
