package com.example.hazardreporting.data.session.dto

import kotlinx.serialization.Serializable

/**
 * Successful response of `POST /login`
 *
 * @property token bearer token of the new session
 * @property user the signed-in user
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class LoginResponseDto(
    val token: String,
    val user: UserDto,
)
