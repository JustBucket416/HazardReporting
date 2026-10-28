package com.example.hazardreporting.di

import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.domain.repository.SessionRepository
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
    fun provideLoginInteractor(sessionRepository: SessionRepository): LoginInteractor =
        LoginInteractor(sessionRepository)
}
