package com.example.hazardreporting.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.Multibinds

/**
 * Binds the [ViewModelFactory] and every ViewModel into the map it creates them from
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
interface ViewModelModule {

    /**
     * Binds [ViewModelFactory] as the app's [ViewModelProvider.Factory]
     */
    @Binds
    fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    /**
     * Declares the ViewModel map, so it can be injected even while it is empty
     */
    @Multibinds
    fun viewModels(): Map<Class<out ViewModel>, ViewModel>
}
