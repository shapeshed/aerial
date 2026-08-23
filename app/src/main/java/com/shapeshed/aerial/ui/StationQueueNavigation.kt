package com.shapeshed.aerial.ui

import com.shapeshed.aerial.data.Station

/** Returns a neighbouring station in the active playback order, wrapping at either end. */
internal fun List<Station>.stationAtOffset(current: Station, offset: Int): Station? {
    if (size < 2) return null
    val currentIndex = indexOfFirst { it.matches(current) }
    if (currentIndex < 0) return null
    return this[(currentIndex + offset).mod(size)]
}
