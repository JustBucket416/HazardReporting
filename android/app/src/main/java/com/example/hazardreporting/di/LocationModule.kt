package com.example.hazardreporting.di

import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides

/**
 * Provides the fused location provider client and the system's location manager
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

    /**
     * Provides the system's location manager
     */
    @Provides
    fun provideLocationManager(context: Context): LocationManager =
        context.getSystemService(LocationManager::class.java)
}
