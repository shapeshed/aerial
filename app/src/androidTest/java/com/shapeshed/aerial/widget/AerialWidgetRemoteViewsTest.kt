package com.shapeshed.aerial.widget

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shapeshed.aerial.AerialApp
import com.shapeshed.aerial.R
import com.shapeshed.aerial.data.Station
import com.shapeshed.aerial.testing.AerialTestEnvironment
import com.shapeshed.aerial.testing.AerialTestEnvironmentRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AerialWidgetRemoteViewsTest {
    @get:Rule
    val testEnvironment = AerialTestEnvironmentRule()

    @Test
    fun everyResponsiveLayoutInflatesWithoutUnsupportedActions() {
        listOf(
            LayoutCase(R.layout.widget_player_stick, hasArtwork = false, hasText = false),
            LayoutCase(R.layout.widget_player_narrow, hasArtwork = true, hasText = false),
            LayoutCase(R.layout.widget_player, hasArtwork = true, hasText = false),
            LayoutCase(R.layout.widget_player_expanded, hasArtwork = true, hasText = true),
        ).forEach { layout ->
            assertNotNull(inflateWidget(layout = layout))
        }
    }

    @Test
    fun textLayoutsUseStableEllipsisWithoutSelectingRemoteText() {
        val widget = inflateWidget(
            layout = LayoutCase(R.layout.widget_player_expanded, hasArtwork = true, hasText = true),
            title = "A very long programme title that cannot fit in the widget",
            subtitle = "A very long artist name that cannot fit in the widget",
        )

        val title = widget.findViewById<TextView>(R.id.widget_station_name)
        val subtitle = widget.findViewById<TextView>(R.id.widget_live_radio)
        assertEquals(TextUtils.TruncateAt.END, title.ellipsize)
        assertEquals(TextUtils.TruncateAt.END, subtitle.ellipsize)
        assertFalse(title.isSelected)
        assertFalse(subtitle.isSelected)
    }

    @Test
    fun compactLayoutFadesArtworkBehindOpaqueControls() {
        val widget = inflateWidget(
            layout = LayoutCase(R.layout.widget_player_narrow, hasArtwork = true, hasText = false),
        )

        val artwork = widget.findViewById<ImageView>(R.id.widget_station_artwork)
        assertEquals(0.45f, artwork.alpha, 0.001f)
        assertNotNull(widget.findViewById<ImageButton>(R.id.widget_previous).background)
        assertNotNull(widget.findViewById<ImageButton>(R.id.widget_play_pause).background)
        assertNotNull(widget.findViewById<ImageButton>(R.id.widget_next).background)
    }

    @Test
    fun defaultLayoutUsesAuxioStyleDockedArtworkAndControls() {
        val widget = inflateWidget(
            layout = LayoutCase(R.layout.widget_player, hasArtwork = true, hasText = false),
        )

        assertNotNull(widget.findViewById<ImageView>(R.id.widget_aspect_ratio))
        assertNotNull(widget.findViewById<ImageView>(R.id.widget_station_artwork))
        val controls = widget.findViewById<View>(R.id.widget_controls)
        val expectedRadius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.resources.getDimension(android.R.dimen.system_app_widget_background_radius)
        } else {
            16 * app.resources.displayMetrics.density
        }
        assertEquals(expectedRadius, (controls.background as GradientDrawable).cornerRadius, 0.5f)
        assertEquals(null, widget.findViewById<TextView>(R.id.widget_station_name))
        assertEquals(null, widget.findViewById<TextView>(R.id.widget_live_radio))
    }

    @Test
    fun controlsReflectPlaybackAndQueueState() {
        val playing = inflateWidget(
            layout = LayoutCase(R.layout.widget_player_narrow, hasArtwork = true, hasText = false),
            playback = playback(isPlaying = true, hasNavigation = true),
        )
        val paused = inflateWidget(
            layout = LayoutCase(R.layout.widget_player_narrow, hasArtwork = true, hasText = false),
            playback = playback(isPlaying = false, hasNavigation = false),
        )

        val playingButton = playing.findViewById<ImageButton>(R.id.widget_play_pause)
        val pausedButton = paused.findViewById<ImageButton>(R.id.widget_play_pause)
        assertEquals(app.getString(R.string.widget_pause), playingButton.contentDescription)
        assertEquals(app.getString(R.string.widget_play), pausedButton.contentDescription)
        assertTrue(cornerRadius(playingButton) < cornerRadius(pausedButton))
        assertEquals(View.VISIBLE, playing.findViewById<View>(R.id.widget_previous).visibility)
        assertEquals(View.VISIBLE, playing.findViewById<View>(R.id.widget_next).visibility)
        assertEquals(View.INVISIBLE, paused.findViewById<View>(R.id.widget_previous).visibility)
        assertEquals(View.INVISIBLE, paused.findViewById<View>(R.id.widget_next).visibility)
    }

    private val app: AerialApp
        get() = AerialTestEnvironment.app()

    private fun inflateWidget(
        layout: LayoutCase,
        title: String = "Programme title",
        subtitle: String = "Artist name",
        playback: WidgetPlaybackState = playback(isPlaying = true, hasNavigation = true),
    ): View {
        lateinit var inflated: View
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val artwork = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.MAGENTA)
            }
            inflated = createWidgetViews(
                app = app,
                layoutId = layout.id,
                station = station,
                artwork = artwork,
                displayTitle = title,
                displaySubtitle = subtitle,
                playback = playback,
                hasArtwork = layout.hasArtwork,
                hasText = layout.hasText,
            ).apply(app, FrameLayout(app))
        }
        return inflated
    }

    private fun cornerRadius(button: ImageButton): Float =
        (button.background as GradientDrawable).cornerRadius

    private fun playback(isPlaying: Boolean, hasNavigation: Boolean) = WidgetPlaybackState(
        mediaId = station.id.toString(),
        isPlaying = isPlaying,
        trackTitle = "Programme title",
        trackArtist = "Artist name",
        canSkipPrevious = hasNavigation,
        canSkipNext = hasNavigation,
    )

    private val station = Station(
        id = 42,
        name = "Test station",
        streamUrl = "https://example.invalid/radio",
        isFavorite = true,
    )

    private data class LayoutCase(
        val id: Int,
        val hasArtwork: Boolean,
        val hasText: Boolean,
    )
}
