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
                "countrycode": "GB",
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
                countrycode = "GB",
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
        assertEquals("", station.countrycode)
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
            station(name = "Low score", url = "https://example.com/stream?unused=true", votes = 1, clickcount = 1),
            station(name = "High score", url = "http://example.com/stream/", votes = 50, clickcount = 10),
        )

        val result = RadioBrowserApi.deduplicate(stations)

        assertEquals(1, result.size)
        assertEquals("High score", result.single().name)
    }

    @Test
    fun deduplicateCopiesAvailableFaviconFromDuplicateGroup() {
        val stations = listOf(
            station(name = "Winner without favicon", url = "https://example.com/stream", votes = 100, favicon = ""),
            station(name = "Duplicate with favicon", url = "http://example.com/stream/", votes = 1, favicon = "https://example.com/icon.png"),
        )

        val result = RadioBrowserApi.deduplicate(stations)

        assertEquals("https://example.com/icon.png", result.single().favicon)
    }

    @Test
    fun deduplicateDropsStationsWithoutResolvedUrl() {
        val result = RadioBrowserApi.deduplicate(listOf(station(name = "Missing URL", url = "")))
        assertEquals(emptyList<RadioBrowserStation>(), result)
    }

    // Station name cleaning

    @Test
    fun parseSearchResponseStripsSquareBracketFormatSuffix() {
        assertNameCleaned("Heart London 106.2 [MP3]", "Heart London 106.2")
        assertNameCleaned("Radio Station [AAC]", "Radio Station")
        assertNameCleaned("Station [128k]", "Station")
    }

    @Test
    fun parseSearchResponseStripsBitrateInParens() {
        assertNameCleaned("BBC Radio 4 (128k)", "BBC Radio 4")
        assertNameCleaned("ByteFM (192k)", "ByteFM")
        assertNameCleaned("Station (320kbps)", "Station")
    }

    @Test
    fun parseSearchResponseStripsFormatInParens() {
        assertNameCleaned("Radio X London (MP3)", "Radio X London")
        assertNameCleaned("Antenne Bayern - Classic Rock Live (MP3)", "Antenne Bayern - Classic Rock Live")
        assertNameCleaned("Station (AAC)", "Station")
        assertNameCleaned("Station (HQ)", "Station")
    }

    @Test
    fun parseSearchResponseStripsPipeSeparatedTechnicalSuffix() {
        assertNameCleaned("Deutschlandfunk | DLF | MP3 128k", "Deutschlandfunk")
        assertNameCleaned("Deutschlandfunk Kultur | DLF | MP3 128k", "Deutschlandfunk Kultur")
        assertNameCleaned("Deutschlandfunk | DLF | OPUS 24k", "Deutschlandfunk")
        assertNameCleaned("Deutschlandfunk | DLF | AAC 48k", "Deutschlandfunk")
    }

    @Test
    fun parseSearchResponseStripsDashPrefixedFormatSuffix() {
        assertNameCleaned("TranceBase.FM - AAC HD 256k", "TranceBase.FM")
        assertNameCleaned("Station - FLAC", "Station")
        assertNameCleaned("Station - MP3 128k", "Station")
    }

    @Test
    fun parseSearchResponseStripsBareFormatAndBitrate() {
        assertNameCleaned("TechnoBase.FM   AAC HD 256k", "TechnoBase.FM")
        assertNameCleaned("SWR1 BW neu (2021.12) 128k mp3", "SWR1 BW neu (2021.12)")
        assertNameCleaned("LBC London (London stream) MP3", "LBC London (London stream)")
    }

    @Test
    fun parseSearchResponseStripsBareQualityWord() {
        assertNameCleaned("90s90s Dance HQ", "90s90s Dance")
        assertNameCleaned("WDR 3 HQ", "WDR 3")
        assertNameCleaned("90s90s Techno HQ", "90s90s Techno")
    }

    @Test
    fun parseSearchResponsePreservesLegitimateGenreSuffixes() {
        assertNameCleaned("Rock Antenne - Heavy Metal", "Rock Antenne - Heavy Metal")
        assertNameCleaned("Sunshine Live - Die 90er", "Sunshine Live - Die 90er")
        assertNameCleaned("Rock Antenne - Chillout", "Rock Antenne - Chillout")
    }

    @Test
    fun parseSearchResponsePreservesCleanNames() {
        assertNameCleaned("1LIVE", "1LIVE")
        assertNameCleaned("Radio 4 Extra", "Radio 4 Extra")
        assertNameCleaned("80s80s", "80s80s")
        assertNameCleaned("MANGORADIO", "MANGORADIO")
        assertNameCleaned("BBC Radio 6 Music", "BBC Radio 6 Music")
        assertNameCleaned("1Mix Radio - Trance (UK)", "1Mix Radio - Trance (UK)")
    }

    private fun assertNameCleaned(input: String, expected: String) {
        val json = """
            [{
                "stationuuid": "test",
                "name": "$input",
                "url_resolved": "https://example.com/stream"
            }]
        """.trimIndent()
        assertEquals(expected, RadioBrowserApi.parseSearchResponse(json).single().name)
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
        countrycode = "GB",
        tags = "radio",
        favicon = favicon,
    )
}
