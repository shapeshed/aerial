package com.shapeshed.aerial.widget

import android.content.Context
import android.util.Log
import com.shapeshed.aerial.AerialApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

const val ACTION_WIDGET_PREVIOUS = "com.shapeshed.aerial.action.WIDGET_PREVIOUS"
const val ACTION_WIDGET_TOGGLE = "com.shapeshed.aerial.action.WIDGET_TOGGLE"
const val ACTION_WIDGET_NEXT = "com.shapeshed.aerial.action.WIDGET_NEXT"

private val updateGeneration = AtomicLong()

internal data class WidgetNavigationAvailability(
    val previous: Boolean,
    val next: Boolean,
)

internal fun widgetNavigationAvailability(index: Int, size: Int): WidgetNavigationAvailability =
    WidgetNavigationAvailability(
        previous = index in 0 until size && size > 1,
        next = index in 0 until size && size > 1,
    )

fun requestAerialWidgetUpdate(context: Context) {
    val app = context.applicationContext as? AerialApp ?: return
    val generation = updateGeneration.incrementAndGet()
    app.applicationScope.launch {
        delay(UPDATE_DEBOUNCE_MS)
        if (generation != updateGeneration.get()) return@launch
        try {
            updateAerialWidgets(app) { generation == updateGeneration.get() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Log.e("AerialWidget", "Widget update failed", error)
        }
    }
}

private const val UPDATE_DEBOUNCE_MS = 150L
