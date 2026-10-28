package com.example.hazardreporting

import android.app.Application
import com.example.hazardreporting.di.AppComponent
import com.example.hazardreporting.di.DaggerAppComponent

/**
 * An [Application] class that builds the Dagger graph
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
    }
}
