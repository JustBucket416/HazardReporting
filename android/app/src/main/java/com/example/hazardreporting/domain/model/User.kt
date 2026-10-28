package com.example.hazardreporting.domain.model

/**
 * A signed-in person
 *
 * @property id server-generated user id
 * @property email email the user signs in with
 * @property name display name
 * @property role what the user does in the organisation
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
)
