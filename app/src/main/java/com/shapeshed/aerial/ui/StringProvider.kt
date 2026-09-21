package com.shapeshed.aerial.ui

import androidx.annotation.StringRes

/** Supplies localized strings without exposing an Android Context to state holders. */
fun interface StringProvider {
    fun get(@StringRes id: Int): String
}
