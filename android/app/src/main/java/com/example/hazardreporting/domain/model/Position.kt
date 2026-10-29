package com.example.hazardreporting.domain.model

/**
 * The device's geographic coordinates when an incident was reported
 *
 * @property lat latitude in degrees
 * @property lng longitude in degrees
 * @property accuracyM radius of the 68% confidence circle, in metres
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class Position(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
)
