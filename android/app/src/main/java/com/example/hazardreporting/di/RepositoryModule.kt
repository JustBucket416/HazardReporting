package com.example.hazardreporting.di

import com.example.hazardreporting.data.catalog.CatalogRepositoryImpl
import com.example.hazardreporting.data.incident.IncidentRepositoryImpl
import com.example.hazardreporting.data.location.FusedLocationProvider
import com.example.hazardreporting.data.session.SessionRepositoryImpl
import com.example.hazardreporting.data.sync.SyncSchedulerImpl
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.example.hazardreporting.domain.repository.IncidentRepository
import com.example.hazardreporting.domain.repository.LocationProvider
import com.example.hazardreporting.domain.repository.SessionRepository
import com.example.hazardreporting.domain.repository.SyncScheduler
import dagger.Binds
import dagger.Module

/**
 * Binds the data-layer repository implementations to their domain interfaces
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
interface RepositoryModule {

    /**
     * Binds [SessionRepositoryImpl] as the [SessionRepository]
     */
    @Binds
    fun bindSessionRepository(repository: SessionRepositoryImpl): SessionRepository

    /**
     * Binds [CatalogRepositoryImpl] as the [CatalogRepository]
     */
    @Binds
    fun bindCatalogRepository(repository: CatalogRepositoryImpl): CatalogRepository

    /**
     * Binds [IncidentRepositoryImpl] as the [IncidentRepository]
     */
    @Binds
    fun bindIncidentRepository(repository: IncidentRepositoryImpl): IncidentRepository

    /**
     * Binds [FusedLocationProvider] as the [LocationProvider]
     */
    @Binds
    fun bindLocationProvider(provider: FusedLocationProvider): LocationProvider

    /**
     * Binds [SyncSchedulerImpl] as the [SyncScheduler]
     */
    @Binds
    fun bindSyncScheduler(scheduler: SyncSchedulerImpl): SyncScheduler
}
