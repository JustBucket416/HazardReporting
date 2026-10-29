package com.example.hazardreporting.domain.repository

import com.example.hazardreporting.domain.model.Position

/**
 * Provides the device's current position
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface LocationProvider {

    /**
     * Returns the device's current position, or null if location permission isn't granted
     * or no fresh enough fix is available. Never throws for a missing fix
     */
    suspend fun currentPosition(): Position?
}
