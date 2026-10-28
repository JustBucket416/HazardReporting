package com.example.hazardreporting.di

import com.example.hazardreporting.data.session.SessionRepositoryImpl
import com.example.hazardreporting.domain.repository.SessionRepository
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
}
