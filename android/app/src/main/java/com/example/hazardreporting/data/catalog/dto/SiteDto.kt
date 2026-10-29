package com.example.hazardreporting.data.catalog.dto

import kotlinx.serialization.Serializable

/**
 * A site as the server sends it
 *
 * @property id server-generated site id
 * @property name display name
 * @property lat latitude of the site
 * @property lng longitude of the site
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class SiteDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
)
