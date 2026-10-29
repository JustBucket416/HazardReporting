package com.example.hazardreporting.data.session.dto

import kotlinx.serialization.Serializable

/**
 * A user as the server sends it
 *
 * @property id server-generated user id
 * @property email email the user signs in with
 * @property name display name
 * @property role `"technician"` or `"operator"`
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
)
