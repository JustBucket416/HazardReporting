package com.example.hazardreporting.data.remote

import javax.inject.Qualifier

/**
 * Qualifies the [okhttp3.HttpUrl] of the server's root. Retrofit is built on it, and converters resolve
 * the server's relative URLs (e.g. `/uploads/ab12.jpg`) against it. Lives in `data`, so the data layer
 * can ask for the URL without depending on `di`, where its value is set
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class BaseUrl
