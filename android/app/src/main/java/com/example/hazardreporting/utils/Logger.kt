package com.example.hazardreporting.utils

import android.util.Log

/**
 * Writes the app's logs to Logcat. JVM unit tests that reach a log call mock it with `mockkObject(Logger)`
 *
 * @author Roman Pliskin on 29.10.2026
 */
object Logger {

    /**
     * Logs an error [message] under [tag], together with the [throwable] that caused it, if any
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
