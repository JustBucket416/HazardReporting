package com.example.hazardreporting.domain.model

/**
 * The rest of an incident beyond its summary
 *
 * @property description what the technician saw; may be empty
 * @property locationDescription where on the site the problem is; may be empty
 * @property position the device's position when the incident was reported, or null if it wasn't available
 * @property photos one to three pictures of the problem, in order
 * @property priority how soon the organisation will act, or null until an operator sets it
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentDetails(
    val description: String,
    val locationDescription: String,
    val position: Position?,
    val photos: List<Photo>,
    val priority: Priority?,
)
