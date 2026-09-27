package com.shapeshed.aerial.di

import javax.inject.Qualifier

/** Marks the dispatcher used for blocking IO (artwork files, database, network). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
