package com.example.hazardreporting.data.session

import com.example.hazardreporting.domain.model.User

/**
 * The signed-in user together with the token that authorises their requests
 *
 * @property token bearer token sent with every request except login
 * @property user the signed-in user
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class Session(
    val token: String,
    val user: User,
) {

    /**
     * Keeps the token out of logs
     */
    override fun toString(): String = "Session(token=<redacted>, user=$user)"
}
