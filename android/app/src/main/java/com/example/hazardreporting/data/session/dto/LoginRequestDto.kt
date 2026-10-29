package com.example.hazardreporting.data.session.dto

import kotlinx.serialization.Serializable

/**
 * Body of `POST /login`
 *
 * @property email email to sign in with
 * @property password plain-text password
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)
