package com.example.hazardreporting.di

import dagger.Module

/**
 * Provides the domain interactors. They are concrete classes without `@Inject`,
 * so the domain layer stays free of DI annotations
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object InteractorModule
