package com.example.hazardreporting.di

import androidx.lifecycle.ViewModel
import com.example.hazardreporting.presentation.MainViewModel
import com.example.hazardreporting.presentation.incidentlist.IncidentListViewModel
import com.example.hazardreporting.presentation.login.LoginViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Binds every ViewModel into the map that [ViewModelComponent] creates them from
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
interface ViewModelModule {

    /**
     * Binds [MainViewModel] into the ViewModel map
     */
    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    /**
     * Binds [LoginViewModel] into the ViewModel map
     */
    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel::class)
    fun bindLoginViewModel(viewModel: LoginViewModel): ViewModel

    /**
     * Binds [IncidentListViewModel] into the ViewModel map
     */
    @Binds
    @IntoMap
    @ViewModelKey(IncidentListViewModel::class)
    fun bindIncidentListViewModel(viewModel: IncidentListViewModel): ViewModel
}
