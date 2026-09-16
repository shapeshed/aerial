package com.shapeshed.aerial.widget

import android.content.Context
import android.content.Intent

const val ACTION_UPDATE_AERIAL_WIDGETS = "com.shapeshed.aerial.action.UPDATE_WIDGETS"

fun requestAerialWidgetUpdate(context: Context) {
    context.sendBroadcast(
        Intent(ACTION_UPDATE_AERIAL_WIDGETS)
            .setPackage(context.packageName),
    )
}
