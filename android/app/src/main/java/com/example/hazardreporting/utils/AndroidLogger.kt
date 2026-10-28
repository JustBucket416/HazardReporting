package com.example.hazardreporting.utils

import android.util.Log
import javax.inject.Inject

/**
 * [Logger] implementation that writes to Logcat
 *
 * @author Roman Pliskin on 29.10.2026
 */
class AndroidLogger @Inject constructor() : Logger {

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
