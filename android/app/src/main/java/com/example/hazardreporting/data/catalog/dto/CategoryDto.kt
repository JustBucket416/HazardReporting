package com.example.hazardreporting.data.catalog.dto

import kotlinx.serialization.Serializable

/**
 * A category as the server sends it
 *
 * @property id server-generated category id
 * @property name display name
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
)
