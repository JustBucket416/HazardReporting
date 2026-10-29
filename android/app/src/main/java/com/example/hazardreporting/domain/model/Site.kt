package com.example.hazardreporting.domain.model

/**
 * A named facility or area the organisation maintains
 *
 * @property id server-generated site id
 * @property name display name
 * @property lat latitude of the site
 * @property lng longitude of the site
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class Site(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
)
