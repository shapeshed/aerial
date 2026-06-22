package com.shapeshed.aerial.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BbcNowPlayingApiTest {

    @Test
    fun resolveBbcServiceIdMatchesRadioOneFromStreamUrl() {
        val station = station(
            name = "BBC Radio 1",
            url = "http://as-hls-ww-live.akamaized.net/pool/01505109/live/ww/bbc_radio_one/bbc_radio_one.isml/bbc_radio_one-audio%3d96000.rewind.m3u8",
        )

        assertEquals("bbc_radio_one", resolveBbcServiceId(station))
    }

    @Test
    fun resolveBbcServiceIdMatchesSportsExtraThreeFromStreamUrl() {
        val station = station(
            name = "Anything",
            url = "https://as-hls-uk-live.akamaized.net/pool_02012018/live/uk/bbc_radio_five_sports_extra_3/bbc_radio_five_sports_extra_3.isml/bbc_radio_five_sports_extra_3-audio%3d320000.norewind.m3u8",
        )

        assertEquals("bbc_radio_five_sports_extra_3", resolveBbcServiceId(station))
    }

    @Test
    fun resolveBbcServiceIdStillFallsBackToStationNameForOlderEntries() {
        val station = station(
            name = "BBC 6 Music",
            url = "https://example.com/live",
        )

        assertEquals("bbc_6music", resolveBbcServiceId(station))
    }

    @Test
    fun parseBbcNowPlayingResponsePrefersNowPlayingItem() {
        val result = parseBbcNowPlayingResponse(
            """
            {
              "total": 2,
              "data": [
                {
                  "titles": {
                    "primary": "Ariana Grande",
                    "secondary": "Supernatural"
                  },
                  "image_url": "https://ichef.bbci.co.uk/images/ic/{recipe}/artwork-old.jpg",
                  "offset": {
                    "now_playing": false
                  }
                },
                {
                  "titles": {
                    "primary": "Tame Impala & JENNIE",
                    "secondary": "Dracula - JENNIE Remix"
                  },
                  "image_url": "https://ichef.bbci.co.uk/images/ic/{recipe}/artwork-now.jpg",
                  "offset": {
                    "now_playing": true
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(
            BbcNowPlayingItem(
                artistTitle = "Tame Impala & JENNIE",
                trackTitle = "Dracula - JENNIE Remix",
                artworkUrl = "https://ichef.bbci.co.uk/images/ic/640x640/artwork-now.jpg",
                artworkData = null,
            ),
            result,
        )
    }

    @Test
    fun parseBbcNowPlayingResponseReturnsNullWhenNoCurrentItemIsMarked() {
        val result = parseBbcNowPlayingResponse(
            """
            {
              "data": [
                {
                  "titles": {
                    "primary": "Simple Minds",
                    "secondary": "Someone Somewhere In Summertime"
                  },
                  "image_url": "https://ichef.bbci.co.uk/images/ic/{recipe}/cover.jpg"
                }
              ]
            }
            """.trimIndent()
        )

        assertNull(result)
    }

    @Test
    fun parseBbcBroadcastResponseUsesCurrentProgrammeArtwork() {
        val result = parseBbcBroadcastResponse(
            """
            {
              "total": 1,
              "data": [
                {
                  "on_air": true,
                  "programme": {
                    "titles": {
                      "primary": "Going Home with Vick, Katie and Jamie on Radio 1",
                      "secondary": "Chaos on your way home!"
                    },
                    "images": [
                      {
                        "url": "https://ichef.bbci.co.uk/images/ic/{recipe}/programme.jpg"
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()
            , Instant.parse("2026-06-22T16:46:00Z")
        )

        assertEquals(
            BbcBroadcastItem(
                showTitle = "Going Home with Vick, Katie and Jamie on Radio 1",
                episodeTitle = "Chaos on your way home!",
                artworkUrl = "https://ichef.bbci.co.uk/images/ic/640x640/programme.jpg",
                artworkData = null,
            ),
            result,
        )
    }

    @Test
    fun parseBbcBroadcastResponseFallsBackToFirstItemWhenNoProgrammeMatchesNow() {
        val result = parseBbcBroadcastResponse(
            """
            {
              "data": [
                {
                  "start": "2026-06-22T10:00:00Z",
                  "end": "2026-06-22T11:00:00Z",
                  "titles": {
                    "primary": "Older Show",
                    "secondary": "Old subtitle"
                  },
                  "image_url": "https://ichef.bbci.co.uk/images/ic/{recipe}/older.jpg"
                }
              ]
            }
            """.trimIndent(),
            Instant.parse("2026-06-22T16:46:00Z")
        )

        assertEquals(
            BbcBroadcastItem(
                showTitle = "Older Show",
                episodeTitle = "Old subtitle",
                artworkUrl = "https://ichef.bbci.co.uk/images/ic/640x640/older.jpg",
                artworkData = null,
            ),
            result,
        )
    }

    @Test
    fun parseBbcNowPlayingResponseReturnsNullForEmptyPayload() {
        assertNull(parseBbcNowPlayingResponse("""{"data": []}"""))
    }

    private fun station(name: String, url: String) = Station(
        name = name,
        streamUrl = url,
    )
}
