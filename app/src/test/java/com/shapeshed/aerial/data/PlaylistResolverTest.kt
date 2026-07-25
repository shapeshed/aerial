package com.shapeshed.aerial.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistResolverTest {

    @Test
    fun isPlaylistUrlDetectsSupportedExtensions() {
        assertTrue(isPlaylistUrl("https://example.com/stream.pls"))
        assertTrue(isPlaylistUrl("https://example.com/stream.m3u"))
        assertTrue(isPlaylistUrl("https://example.com/stream.asx"))
        assertTrue(isPlaylistUrl("https://example.com/STREAM.PLS"))
        assertTrue(isPlaylistUrl("https://example.com/stream.pls?token=abc#frag"))
    }

    @Test
    fun isPlaylistUrlExcludesHlsAndDirectStreams() {
        assertFalse(isPlaylistUrl("https://example.com/master.m3u8"))
        assertFalse(isPlaylistUrl("https://example.com/live.mp3"))
        assertFalse(isPlaylistUrl("https://example.com/live/aac"))
        assertFalse(isPlaylistUrl("https://example.com/stream?type=.pls"))
    }

    @Test
    fun parsesPlsLowestNumberedEntry() {
        val body = """
            [playlist]
            NumberOfEntries=2
            File2=https://example.com/backup.mp3
            File1=https://example.com/primary.mp3
            Title1=Primary
            Version=2
        """.trimIndent()
        assertEquals("https://example.com/primary.mp3", parsePlaylist(body, "https://host/x.pls"))
    }

    @Test
    fun parsesPlsWithWhitespaceAndCrlf() {
        val body = "[playlist]\r\nFile1 = https://example.com/stream.aac \r\nNumberOfEntries=1\r\n"
        assertEquals("https://example.com/stream.aac", parsePlaylist(body, "https://host/x.pls"))
    }

    @Test
    fun parsesM3uSkippingComments() {
        val body = """
            #EXTM3U
            #EXTINF:-1,Some Station
            https://example.com/stream.aac
            https://example.com/second.aac
        """.trimIndent()
        assertEquals("https://example.com/stream.aac", parsePlaylist(body, "https://host/x.m3u"))
    }

    @Test
    fun parsesM3uWithBomAndBlankLines() {
        val body = "﻿\n\n  https://example.com/stream.mp3  \n"
        assertEquals("https://example.com/stream.mp3", parsePlaylist(body, "https://host/x.m3u"))
    }

    @Test
    fun resolvesRelativeM3uEntryAgainstPlaylistUrl() {
        val body = "#EXTM3U\nmount.mp3\n"
        assertEquals(
            "https://example.com/radio/mount.mp3",
            parsePlaylist(body, "https://example.com/radio/listen.m3u"),
        )
    }

    @Test
    fun parsesAsxFirstRefHref() {
        val body = """
            <ASX version="3.0">
              <Entry>
                <Ref HREF="https://example.com/stream.asf" />
                <Ref HREF="https://example.com/backup.asf" />
              </Entry>
            </ASX>
        """.trimIndent()
        assertEquals("https://example.com/stream.asf", parsePlaylist(body, "https://host/x.asx"))
    }

    @Test
    fun parsesAsxWithSingleQuotesAndExtraAttributes() {
        val body = "<asx><entry><ref foo='bar' href='http://example.com/a.mp3' /></entry></asx>"
        assertEquals("http://example.com/a.mp3", parsePlaylist(body, "https://host/x.asx"))
    }

    @Test
    fun fallsBackToBodySniffWhenExtensionMismatchesContent() {
        // A .pls URL whose body is actually an m3u still resolves via the sniff fallback.
        val body = "#EXTM3U\nhttps://example.com/stream.aac\n"
        assertEquals("https://example.com/stream.aac", parsePlaylist(body, "https://host/x.pls"))
    }

    @Test
    fun returnsNullForEmptyOrGarbageBody() {
        assertNull(parsePlaylist("", "https://host/x.pls"))
        assertNull(parsePlaylist("not a playlist at all", "https://host/x.pls"))
        assertNull(parsePlaylist("[playlist]\nNumberOfEntries=0\n", "https://host/x.pls"))
    }

    @Test
    fun resolveStreamUrlPassesThroughNonPlaylist() {
        val direct = "https://example.com/live.mp3"
        assertEquals(direct, resolveStreamUrl(direct) { fail("should not fetch a direct stream") })
    }

    @Test
    fun resolveStreamUrlUnwrapsSinglePlaylist() {
        val resolved = resolveStreamUrl("https://host/x.pls") {
            "[playlist]\nFile1=https://example.com/stream.aac\n"
        }
        assertEquals("https://example.com/stream.aac", resolved)
    }

    @Test
    fun resolveStreamUrlFollowsNestedPlaylistWithinHopLimit() {
        val resolved = resolveStreamUrl("https://host/outer.pls") { url ->
            when (url) {
                "https://host/outer.pls" -> "[playlist]\nFile1=https://host/inner.m3u\n"
                "https://host/inner.m3u" -> "https://example.com/final.aac\n"
                else -> null
            }
        }
        assertEquals("https://example.com/final.aac", resolved)
    }

    @Test
    fun resolveStreamUrlStopsAtHopLimit() {
        // A playlist that only ever points at another playlist bottoms out at the last URL,
        // never looping forever.
        val resolved = resolveStreamUrl("https://host/0.pls", maxHops = 2) { url ->
            val n = url.substringAfterLast('/').substringBefore('.').toInt()
            "[playlist]\nFile1=https://host/${n + 1}.pls\n"
        }
        assertEquals("https://host/2.pls", resolved)
    }

    @Test
    fun resolveStreamUrlReturnsOriginalWhenFetchFails() {
        val url = "https://host/dead.pls"
        assertEquals(url, resolveStreamUrl(url) { null })
    }

    private fun fail(message: String): Nothing = throw AssertionError(message)
}
