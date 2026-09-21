package com.shapeshed.aerial.data

/**
 * Fills a station's artwork in from the bundled registry when that station has no
 * usable logo of its own.
 *
 * Used when building queue and playback-resumption media items: a persisted
 * station may predate a registry logo, so resolving against the registry at play
 * time keeps Android Auto / lock-screen artwork up to date. Prefers the
 * `(provider, providerId)` identity when both are present (provider ids are only
 * unique within a provider), otherwise matches on the stream URL.
 */
class StationArtworkResolver(private val registryRepository: RegistryRepository) {

    /** Returns [station] with its logo path replaced by the registry logo, or the
     * same instance when there is no registry match or the registry logo is blank. */
    suspend fun recover(station: Station): Station {
        val registry = when {
            station.provider.isNotBlank() && station.providerId.isNotBlank() ->
                registryRepository.getByProviderId(station.provider, station.providerId)

            else -> registryRepository.getByStreamUrl(station.streamUrl)
        }
        return registry?.logoUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { station.copy(logoPath = it) }
            ?: station
    }
}
