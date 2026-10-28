package com.example.hazardreporting.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.example.hazardreporting.data.session.SessionHolder
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

/**
 * The application's Dagger component. Built once in [com.example.hazardreporting.HazardReportingApp]
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Singleton
@Component(
    modules = [
        AppModule::class,
        DataStoreModule::class,
        InteractorModule::class,
        NetworkModule::class,
        RepositoryModule::class,
        ViewModelModule::class,
    ]
)
interface AppComponent {

    /**
     * Returns the coroutine scope that lives as long as the application
     */
    @ApplicationScope
    fun applicationScope(): CoroutineScope

    /**
     * Returns the holder of the in-memory session, so the application can load it at startup
     */
    fun sessionHolder(): SessionHolder

    /**
     * Returns the factory that creates every ViewModel
     */
    fun viewModelFactory(): ViewModelProvider.Factory

    /**
     * Creates [AppComponent]
     */
    @Component.Factory
    interface Factory {

        /**
         * Creates [AppComponent] bound to the application [context]
         */
        fun create(@BindsInstance context: Context): AppComponent
    }
}
