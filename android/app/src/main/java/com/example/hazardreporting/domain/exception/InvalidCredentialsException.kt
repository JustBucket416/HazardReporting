package com.example.hazardreporting.domain.exception

/**
 * Thrown when signing in fails because the email or password is wrong
 *
 * @param cause the server's rejection
 *
 * @author Roman Pliskin on 29.10.2026
 */
class InvalidCredentialsException(cause: Throwable? = null) : Exception("Invalid email or password", cause)
