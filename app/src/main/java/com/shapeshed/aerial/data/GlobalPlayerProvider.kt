package com.shapeshed.aerial.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class GlobalPlayerProvider(okHttpClient: OkHttpClient) : Provider {
    private var job: Job? = null
    private var webSocket: WebSocket? = null
    private val client = okHttpClient.newBuilder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    override fun canEnrich(station: Station): Boolean = isGlobalStation(station)

    override fun notifyTransition() {}

    override fun start(station: Station, scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            val heraldId = resolveGlobalHeraldId(station)
            if (heraldId == null) {
                Log.w(TAG, "No heraldId found for ${station.name} (${station.streamUrl})")
                return@launch
            }
            Log.d(TAG, "Subscribing heraldId=$heraldId for ${station.name}")

            val messages = Channel<String>(Channel.UNLIMITED)
            val request = Request.Builder()
                .url("wss://metadata.musicradio.com/v2/now-playing")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("""{"actions":[{"type":"subscribe","service":"$heraldId"}]}""")
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    messages.trySend(text)
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket failure: ${t.message}")
                    messages.close(t)
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    messages.close()
                }
            })

            try {
                for (message in messages) {
                    if (!isActive) break
                    val content = parseGlobalNowPlaying(message) ?: continue
                    NowPlayingStore.set(buildNowPlayingInfo(station.id, content))
                }
            } finally {
                webSocket?.close(1000, null)
                webSocket = null
            }
        }
    }

    override fun pause() {
        job?.cancel()
        job = null
        webSocket?.close(1000, null)
        webSocket = null
    }

    override fun stop() {
        job?.cancel()
        job = null
        webSocket?.close(1000, null)
        webSocket = null
        NowPlayingStore.set(null)
    }

    companion object {
        private const val TAG = "GlobalPlayerStationProvider"
    }
}

private fun buildNowPlayingInfo(stationId: Long, content: GlobalMetadataContent): NowPlayingInfo {
    val track = if (content.trackArtist != null && content.trackTitle != null) {
        TrackInfo(
            artist = content.trackArtist,
            title = content.trackTitle,
            artworkUrl = content.trackArtworkUrl,
            artworkData = content.trackArtworkData,
        )
    } else null
    return NowPlayingInfo(
        stationId = stationId,
        programmeTitle = content.showTitle,
        programmeSubtitle = null,
        artworkUrl = content.showArtworkUrl ?: content.trackArtworkUrl,
        artworkData = content.showArtworkData ?: content.trackArtworkData,
        track = track,
    )
}
