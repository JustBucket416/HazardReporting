package com.example.hazardreporting.data.catalog.dto

import kotlinx.serialization.Serializable

/**
 * A severity as the server sends it
 *
 * @property id server-generated severity id
 * @property name display name
 * @property rank position on the scale: 1 = Low … 4 = Critical
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class SeverityDto(
    val id: String,
    val name: String,
    val rank: Int,
)
