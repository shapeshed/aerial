package com.shapeshed.aerial.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import com.shapeshed.aerial.data.NetworkMonitor
import com.shapeshed.aerial.data.RegistryRepository
import com.shapeshed.aerial.data.StationRepository
import com.shapeshed.aerial.playback.WidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The collaborators instrumentation is allowed to read, and nothing else.
 *
 * Declared here rather than in a test source set because a Hilt `@EntryPoint` has to be
 * aggregated into the same `SingletonComponent` that resolves it at runtime; one declared only
 * for `androidTest` is never wired in, and `EntryPointAccessors` fails with a `ClassCastException`.
 *
 * This is also a useful piece of production documentation: it is the shortest honest answer to
 * "what does this app actually depend on?", and because it is the only way to reach a binding from
 * outside, a collaborator nobody binds shows up as a compile error here.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AerialGraph {
    fun repository(): StationRepository
    fun registryRepository(): RegistryRepository
    fun networkMonitor(): NetworkMonitor
    fun settingsDataStore(): DataStore<Preferences>
    fun stringProvider(): StringProvider
    fun widgetUpdater(): WidgetUpdater
    fun imageLoader(): ImageLoader
}
