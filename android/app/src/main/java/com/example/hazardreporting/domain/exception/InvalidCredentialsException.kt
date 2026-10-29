package com.example.hazardreporting.domain.exception

import java.io.IOException

/**
 * Thrown when signing in fails because the email or password is wrong. An [IOException], so OkHttp can pass it
 * on from an interceptor
 *
 * @author Roman Pliskin on 29.10.2026
 */
class InvalidCredentialsException : IOException("Invalid email or password")
