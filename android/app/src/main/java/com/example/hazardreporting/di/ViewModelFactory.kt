package com.example.hazardreporting.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

/**
 * [ViewModelProvider.Factory] that creates every ViewModel bound with [ViewModelKey]
 *
 * @property creators providers of the bound ViewModels, keyed by their class
 *
 * @author Roman Pliskin on 29.10.2026
 */
class ViewModelFactory @Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val creator = requireNotNull(creators[modelClass]) { "No ViewModel bound for $modelClass" }
        @Suppress("UNCHECKED_CAST")
        return creator.get() as T
    }
}
