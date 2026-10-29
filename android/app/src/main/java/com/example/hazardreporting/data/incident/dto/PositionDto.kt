package com.example.hazardreporting.data.incident.dto

import kotlinx.serialization.Serializable

/**
 * An incident's position as the server sends and receives it
 *
 * @property lat latitude in degrees
 * @property lng longitude in degrees
 * @property accuracyM accuracy in metres
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class PositionDto(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
)
