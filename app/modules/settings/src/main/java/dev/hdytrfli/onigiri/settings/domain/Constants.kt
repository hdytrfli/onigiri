package dev.hdytrfli.onigiri.settings.domain

object Constants {
    // Interface
    /**
     * Multiplier applied to content spacing in the detail screens. Fixed to the former
     * "Comfortable" content-spacing value, so removing the setting leaves everyone's spacing
     * unchanged.
     */
    const val CONTENT_SPACING_SCALE = 1.25f

    // Network
    const val NETWORK_DEFAULT_REQUEST_TIMEOUT = 30_000L
    const val NETWORK_DEFAULT_CONNECT_TIMEOUT = 6_000L
    const val NETWORK_DEFAULT_SOCKET_TIMEOUT = 10_000L
}
