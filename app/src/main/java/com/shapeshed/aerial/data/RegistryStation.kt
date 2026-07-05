package com.shapeshed.aerial.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registry_stations",
    indices = [
        Index(value = ["countryCode"]),
        Index(value = ["provider", "providerId"]),
        Index(value = ["providerId"]),
        Index(value = ["streamUrl"]),
    ],
)
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
