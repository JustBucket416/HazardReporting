package com.example.hazardreporting.utils

/**
 * Logging abstraction, so classes that log can run in JVM unit tests
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface Logger {

    /**
     * Logs an error [message] under [tag], together with the [throwable] that caused it, if any
     */
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
