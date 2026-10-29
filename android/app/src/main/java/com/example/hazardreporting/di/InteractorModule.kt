package com.example.hazardreporting.di

import com.example.hazardreporting.domain.interactor.CatalogInteractor
import com.example.hazardreporting.domain.interactor.IncidentInteractor
import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.example.hazardreporting.domain.repository.DeviceDataRepository
import com.example.hazardreporting.domain.repository.IncidentRepository
import com.example.hazardreporting.domain.repository.LocationProvider
import com.example.hazardreporting.domain.repository.SessionRepository
import com.example.hazardreporting.domain.repository.SyncRepository
import com.example.hazardreporting.domain.repository.SyncScheduler
import dagger.Module
import dagger.Provides

/**
 * Provides the domain interactors. They are concrete classes without `@Inject`,
 * so the domain layer stays free of DI annotations
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object InteractorModule {

    /**
     * Provides the [LoginInteractor]
     */
    @Provides
    fun provideLoginInteractor(
        sessionRepository: SessionRepository,
        syncRepository: SyncRepository,
        syncScheduler: SyncScheduler,
        deviceDataRepository: DeviceDataRepository,
    ): LoginInteractor = LoginInteractor(sessionRepository, syncRepository, syncScheduler, deviceDataRepository)

    /**
     * Provides the [CatalogInteractor]
     */
    @Provides
    fun provideCatalogInteractor(catalogRepository: CatalogRepository): CatalogInteractor =
        CatalogInteractor(catalogRepository)

    /**
     * Provides the [IncidentInteractor]
     */
    @Provides
    fun provideIncidentInteractor(
        incidentRepository: IncidentRepository,
        catalogRepository: CatalogRepository,
        syncRepository: SyncRepository,
        sessionRepository: SessionRepository,
        locationProvider: LocationProvider,
        syncScheduler: SyncScheduler,
    ): IncidentInteractor = IncidentInteractor(
        incidentRepository,
        catalogRepository,
        syncRepository,
        sessionRepository,
        locationProvider,
        syncScheduler,
    )
}
