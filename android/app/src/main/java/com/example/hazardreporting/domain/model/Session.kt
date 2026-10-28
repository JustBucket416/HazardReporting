package com.example.hazardreporting.domain.model

/**
 * A session on the server: the signed-in user together with the token that authorises their requests.
 * The domain sees it so that [com.example.hazardreporting.domain.interactor.LoginInteractor] can decide
 * whether to keep a new session before anything is stored
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
