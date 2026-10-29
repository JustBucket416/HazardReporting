package com.example.hazardreporting.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkerFactory
import coil3.ImageLoader
import com.example.hazardreporting.data.session.SessionHolder
import com.example.hazardreporting.domain.repository.SyncScheduler
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
        DatabaseModule::class,
        DataStoreModule::class,
        ImageLoaderModule::class,
        InteractorModule::class,
        LocationModule::class,
        NetworkModule::class,
        RepositoryModule::class,
        ViewModelFactoryModule::class,
        WorkModule::class,
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
     * Returns Coil's image loader, which the application hands to Coil as its singleton
     */
    fun imageLoader(): ImageLoader

    /**
     * Returns the factory that creates every ViewModel
     */
    fun viewModelFactory(): ViewModelProvider.Factory

    /**
     * Returns the factory that creates every WorkManager worker
     */
    fun workerFactory(): WorkerFactory

    /**
     * Returns the scheduler of background pushes, so the application can keep the periodic push scheduled
     */
    fun syncScheduler(): SyncScheduler

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
