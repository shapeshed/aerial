package com.shapeshed.aerial.ui

import android.provider.Settings
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** [basicMarquee] ignores the system "Remove animations" accessibility setting (animator
 * duration scale == 0) — unlike native Android views, which respect it everywhere. That setting
 * exists for users who get physically sick from motion, so check it first and no-op otherwise,
 * leaving whatever ellipsis/wrap behavior the Text already has. */
@Composable
fun Modifier.safeMarquee(): Modifier {
    val context = LocalContext.current
    val animationsRemoved = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return if (animationsRemoved) this else this.basicMarquee()
}
