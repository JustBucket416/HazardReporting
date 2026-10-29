package com.example.hazardreporting.di

import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module

/**
 * Installs [ViewModelComponent] in the application graph and binds the [ViewModelFactory] that uses it
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module(subcomponents = [ViewModelComponent::class])
interface ViewModelFactoryModule {

    /**
     * Binds [ViewModelFactory] as the app's [ViewModelProvider.Factory]
     */
    @Binds
    fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
