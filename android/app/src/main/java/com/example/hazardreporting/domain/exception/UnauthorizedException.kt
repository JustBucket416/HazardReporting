package com.example.hazardreporting.domain.exception

/**
 * Thrown when the server no longer accepts the session, e.g. after the server was reset
 *
 * @param cause the server's rejection
 *
 * @author Roman Pliskin on 29.10.2026
 */
class UnauthorizedException(cause: Throwable? = null) : Exception("The session is no longer valid", cause)
