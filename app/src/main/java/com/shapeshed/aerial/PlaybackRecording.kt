package com.shapeshed.aerial

import com.shapeshed.aerial.data.Station

/**
 * Identity used to count a listen once per station, no matter how many media items, buffering
 * pauses or repeat-mode transitions a single tune-in produces.
 *
 * Deliberately not the Media3 mediaId: every unsaved station played from the phone shares the
 * mediaId "0", which would collapse different stations into one and lose their play counts.
 */
internal fun stationPlaybackKey(station: Station): String =
    "${station.provider}|${station.providerId}|${station.streamUrl}"
