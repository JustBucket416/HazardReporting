package com.example.hazardreporting.domain.exception

import java.io.IOException

/**
 * Thrown when the server no longer accepts the session, e.g. after the server was reset. An [IOException],
 * so OkHttp can pass it on from an interceptor
 *
 * @author Roman Pliskin on 29.10.2026
 */
class UnauthorizedException : IOException("The session is no longer valid")
