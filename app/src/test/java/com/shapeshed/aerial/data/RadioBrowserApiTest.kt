package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.json.JSONException
import org.junit.Test

class RadioBrowserApiTest {

    @Test
    fun parseSearchResponseMapsRadioBrowserFields() {
        val result = RadioBrowserApi.parseSearchResponse(
            """
            [
              {
                "stationuuid": "bbc-radio-6",
                "name": "BBC Radio 6 Music",
                "url_resolved": "https://stream.example.com/6music",
                "url": "https://fallback.example.com/6music",
                "votes": 120,
                "clickcount": 35,
                "codec": "MP3",
                "bitrate": 128,
                "country": "United Kingdom",
                "tags": "alternative,indie",
                "favicon": "https://example.com/6music.png"
              }
            ]
            """.trimIndent()
        )

        assertEquals(
            RadioBrowserStation(
                stationuuid = "bbc-radio-6",
                name = "BBC Radio 6 Music",
                urlResolved = "https://stream.example.com/6music",
                votes = 120,
                clickcount = 35,
                codec = "MP3",
                bitrate = 128,
                country = "United Kingdom",
                tags = "alternative,indie",
                favicon = "https://example.com/6music.png",
            ),
            result.single(),
        )
    }

    @Test
    fun parseSearchResponseFallsBackToUrlWhenResolvedUrlIsMissing() {
        val result = RadioBrowserApi.parseSearchResponse(
            """
            [
              {
                "stationuuid": "fallback-url",
                "name": "Fallback URL",
                "url": "https://stream.example.com/live",
                "votes": 4,
                "clickcount": 9
              }
            ]
            """.trimIndent()
        )

        assertEquals("https://stream.example.com/live", result.single().urlResolved)
    }

    @Test
    fun parseSearchResponseUsesDefaultsForMissingOptionalFields() {
        val result = RadioBrowserApi.parseSearchResponse(
            """
            [
              {
                "stationuuid": "minimal",
                "name": "Minimal Station",
                "url_resolved": "https://stream.example.com/minimal"
              }
            ]
            """.trimIndent()
        )

        val station = result.single()
        assertEquals(0, station.votes)
        assertEquals(0, station.clickcount)
        assertEquals(0, station.bitrate)
        assertEquals("", station.codec)
        assertEquals("", station.country)
        assertEquals("", station.tags)
        assertEquals("", station.favicon)
    }

    @Test
    fun parseSearchResponseDeduplicatesAndSortsByScore() {
        val result = RadioBrowserApi.parseSearchResponse(
            """
            [
              {
                "stationuuid": "duplicate-low",
                "name": "Duplicate Low",
                "url_resolved": "https://example.com/live?tracking=1",
                "votes": 1,
                "clickcount": 1,
                "favicon": "https://example.com/icon.png"
              },
              {
                "stationuuid": "distinct",
                "name": "Distinct",
                "url_resolved": "https://example.com/other",
                "votes": 5,
                "clickcount": 0
              },
              {
                "stationuuid": "duplicate-high",
                "name": "Duplicate High",
                "url_resolved": "http://example.com/live/",
                "votes": 10,
                "clickcount": 5
              }
            ]
            """.trimIndent()
        )

        assertEquals(listOf("Duplicate High", "Distinct"), result.map { it.name })
        assertEquals("https://example.com/icon.png", result.first().favicon)
    }

    @Test(expected = JSONException::class)
    fun parseSearchResponseRejectsMalformedJson() {
        RadioBrowserApi.parseSearchResponse("not json")
    }

    @Test
    fun deduplicateKeepsHighestScoredStationForEquivalentUrls() {
        val stations = listOf(
            station(
                name = "Low score",
                url = "https://example.com/stream?unused=true",
                votes = 1,
                clickcount = 1,
            ),
            station(
                name = "High score",
                url = "http://example.com/stream/",
                votes = 50,
                clickcount = 10,
            ),
        )

        val result = RadioBrowserApi.deduplicate(stations)

        assertEquals(1, result.size)
        assertEquals("High score", result.single().name)
    }

    @Test
    fun deduplicateCopiesAvailableFaviconFromDuplicateGroup() {
        val stations = listOf(
            station(
                name = "Winner without favicon",
                url = "https://example.com/stream",
                votes = 100,
                favicon = "",
            ),
            station(
                name = "Duplicate with favicon",
                url = "http://example.com/stream/",
                votes = 1,
                favicon = "https://example.com/icon.png",
            ),
        )

        val result = RadioBrowserApi.deduplicate(stations)

        assertEquals("https://example.com/icon.png", result.single().favicon)
    }

    @Test
    fun deduplicateDropsStationsWithoutResolvedUrl() {
        val result = RadioBrowserApi.deduplicate(
            listOf(station(name = "Missing URL", url = ""))
        )

        assertEquals(emptyList<RadioBrowserStation>(), result)
    }

    private fun station(
        name: String,
        url: String,
        votes: Int = 0,
        clickcount: Int = 0,
        favicon: String = "",
    ) = RadioBrowserStation(
        stationuuid = name.lowercase().replace(' ', '-'),
        name = name,
        urlResolved = url,
        votes = votes,
        clickcount = clickcount,
        codec = "MP3",
        bitrate = 128,
        country = "United Kingdom",
        tags = "radio",
        favicon = favicon,
    )
}
