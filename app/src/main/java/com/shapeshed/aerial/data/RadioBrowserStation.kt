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
    val countrycode: String,
    val tags: String,
    val favicon: String,
) {
    val score: Int get() = votes * 2 + clickcount

    @Suppress("DEPRECATION")
    fun displayCountry(): String {
        if (countrycode.isNotEmpty()) {
            val name = java.util.Locale("", countrycode).getDisplayCountry(java.util.Locale.ENGLISH)
            if (name.isNotEmpty() && name != countrycode) return name
        }
        return country
    }
}
