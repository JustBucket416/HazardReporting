package com.example.hazardreporting.di

import javax.inject.Qualifier

/**
 * Qualifies the [kotlinx.coroutines.CoroutineScope] that lives as long as the application.
 * Work launched in it outlives any screen, e.g. loading the session at startup
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
