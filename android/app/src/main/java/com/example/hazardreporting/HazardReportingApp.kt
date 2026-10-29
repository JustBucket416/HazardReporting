package com.example.hazardreporting

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.example.hazardreporting.di.AppComponent
import com.example.hazardreporting.di.DaggerAppComponent
import kotlinx.coroutines.launch

/**
 * An [Application] class that builds the Dagger graph and starts loading the session.
 * It also gives Coil the graph's [ImageLoader], so every image loads through the app's OkHttp client
 *
 * @author Roman Pliskin on 29.10.2026
 */
class HazardReportingApp : Application(), SingletonImageLoader.Factory {

    /**
     * The application's Dagger component, built in [onCreate]
     */
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
        // Loads the session off the main thread, so the first request rarely has to wait for it (ADR-0010)
        val sessionHolder = appComponent.sessionHolder()
        appComponent.applicationScope().launch { sessionHolder.load() }
    }

    // Coil asks for its loader on the first image load, after onCreate built the graph
    override fun newImageLoader(context: PlatformContext): ImageLoader = appComponent.imageLoader()
}
