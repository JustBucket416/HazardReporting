package com.example.hazardreporting.domain.exception

import java.io.IOException

/**
 * Thrown when the server can't be reached. An [IOException], so OkHttp can pass it on from an interceptor
 *
 * @param cause the transport failure
 *
 * @author Roman Pliskin on 29.10.2026
 */
class NoConnectionException(cause: Throwable? = null) : IOException("The server can't be reached", cause)
