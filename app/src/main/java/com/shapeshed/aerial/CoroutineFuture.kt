package com.shapeshed.aerial

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val DIRECT_EXECUTOR = Executor(Runnable::run)

internal fun <T> CoroutineScope.asServiceFuture(block: suspend () -> T): ListenableFuture<T> {
    val future = SettableFuture.create<T>()
    val job = launch {
        try {
            future.set(block())
        } catch (error: CancellationException) {
            future.cancel(false)
            throw error
        } catch (error: Exception) {
            future.setException(error)
        }
    }
    job.invokeOnCompletion { cause ->
        if (cause is CancellationException) future.cancel(false)
    }
    future.addListener(
        { if (future.isCancelled) job.cancel() },
        DIRECT_EXECUTOR,
    )
    return future
}
