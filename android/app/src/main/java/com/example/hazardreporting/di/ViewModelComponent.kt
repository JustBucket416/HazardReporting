package com.example.hazardreporting.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Provider

/**
 * Dagger subcomponent that creates one ViewModel. [ViewModelFactory] builds a new instance for every
 * ViewModel it creates, bound to that ViewModel's own [SavedStateHandle], so any ViewModel can take
 * the handle as an ordinary constructor parameter, next to its singleton dependencies
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Subcomponent(modules = [ViewModelModule::class])
interface ViewModelComponent {

    /**
     * Returns providers of every bound ViewModel, keyed by their class
     */
    fun viewModels(): Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>

    /**
     * Creates [ViewModelComponent]
     */
    @Subcomponent.Factory
    interface Factory {

        /**
         * Creates [ViewModelComponent] for a ViewModel whose saved state is [savedStateHandle]
         */
        fun create(@BindsInstance savedStateHandle: SavedStateHandle): ViewModelComponent
    }
}
