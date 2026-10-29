package com.example.hazardreporting.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides

/**
 * Provides the fused location provider client
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object LocationModule {

    /**
     * Provides the client of the fused location provider, bound to the application context
     */
    @Provides
    fun provideFusedLocationProviderClient(context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
}
