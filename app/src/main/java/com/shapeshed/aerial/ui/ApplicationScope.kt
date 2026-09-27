package com.shapeshed.aerial.ui

import javax.inject.Qualifier

/**
 * Marks the [kotlinx.coroutines.CoroutineScope] that lives for the whole process.
 *
 * Used for work that must outlive any screen or service, such as coalescing home-screen widget
 * redraws. Project-local rather than Hilt's own qualifier so it sits beside [IoDispatcher] and
 * the two read the same way.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
