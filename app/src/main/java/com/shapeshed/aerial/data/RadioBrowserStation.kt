package com.shapeshed.aerial.data

data class RadioBrowserStation(
    val stationuuid: String,
    val name: String,
    val urlResolved: String,
    val votes: Int,
    val clickcount: Int,
    val codec: String,
    val bitrate: Int,
    val country: String,
    val tags: String,
    val favicon: String,
) {
    val score: Int get() = votes * 2 + clickcount
}
