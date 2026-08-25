package com.shapeshed.aerial.data

import org.junit.Assert.assertTrue
import org.junit.Test

class StreamUrlTest {
    @Test
    fun bauerSharpStreamGetsFreshShortLivedKey() {
        val station = Station(
            name = "KISS",
            streamUrl = "https://live-bauerkiss.sharp-stream.com/kissnational.aac?direct=true",
        )

        val resolved = bauerStreamUrl(station)

        assertTrue(resolved.matches(Regex(".*[?&]aw_0_1st\\.skey=\\d+$")))
    }
}
