package com.example.hazardreporting.domain.exception

/**
 * Thrown when the server can't be reached
 *
 * @param cause the transport failure
 *
 * @author Roman Pliskin on 29.10.2026
 */
class NoConnectionException(cause: Throwable? = null) : Exception("The server can't be reached", cause)
