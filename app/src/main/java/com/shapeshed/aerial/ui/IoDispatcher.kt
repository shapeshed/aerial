package com.shapeshed.aerial.ui

import javax.inject.Qualifier

/** Marks the dispatcher used for blocking IO (artwork files, database, network). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
