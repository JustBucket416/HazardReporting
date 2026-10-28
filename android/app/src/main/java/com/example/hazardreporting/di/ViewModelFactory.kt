package com.example.hazardreporting.di

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import javax.inject.Inject

/**
 * [ViewModelProvider.Factory] that creates every ViewModel bound with [ViewModelKey]. Each ViewModel gets
 * its own [SavedStateHandle], taken from the [CreationExtras] of its owner (an activity or a navigation
 * back stack entry, whose handle also carries the route's arguments)
 *
 * @property viewModelComponentFactory creates the subcomponent that builds one ViewModel
 *
 * @author Roman Pliskin on 29.10.2026
 */
class ViewModelFactory @Inject constructor(
    private val viewModelComponentFactory: ViewModelComponent.Factory,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        create(modelClass, extras.createSavedStateHandle())

    /**
     * Creates the ViewModel of [modelClass] with [savedStateHandle] as its saved state
     *
     * @throws IllegalArgumentException if no ViewModel of [modelClass] is bound
     */
    @VisibleForTesting
    internal fun <T : ViewModel> create(modelClass: Class<T>, savedStateHandle: SavedStateHandle): T {
        val creators = viewModelComponentFactory.create(savedStateHandle).viewModels()
        val creator = requireNotNull(creators[modelClass]) { "No ViewModel bound for $modelClass" }
        @Suppress("UNCHECKED_CAST")
        return creator.get() as T
    }
}
