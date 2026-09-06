package com.shapeshed.aerial.ui

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.shapeshed.aerial.PlayerService

/** Platform boundary for creating and releasing the phone-side media controller. */
interface MediaControllerGateway {
    fun connect(context: Context): ListenableFuture<MediaController>

    fun release(future: ListenableFuture<MediaController>)
}

class DefaultMediaControllerGateway : MediaControllerGateway {
    override fun connect(context: Context): ListenableFuture<MediaController> {
        val appContext = context.applicationContext
        return MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlayerService::class.java)),
        ).buildAsync()
    }

    override fun release(future: ListenableFuture<MediaController>) {
        MediaController.releaseFuture(future)
    }
}
