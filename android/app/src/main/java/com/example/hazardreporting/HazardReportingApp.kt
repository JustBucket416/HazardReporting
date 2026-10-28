package com.example.hazardreporting

import android.app.Application
import com.example.hazardreporting.di.AppComponent
import com.example.hazardreporting.di.DaggerAppComponent
import kotlinx.coroutines.launch

/**
 * An [Application] class that builds the Dagger graph and starts loading the session
 *
 * @author Roman Pliskin on 29.10.2026
 */
class HazardReportingApp : Application() {

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
}
