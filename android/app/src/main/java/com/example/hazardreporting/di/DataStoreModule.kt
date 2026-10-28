package com.example.hazardreporting.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Provides the DataStore that holds the session
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object DataStoreModule {

    private const val SESSION_DATA_STORE_NAME = "session"

    /**
     * Provides the session's preferences DataStore. A corrupted file is replaced with an empty one,
     * which signs the user out instead of crashing the app
     */
    @Provides
    @Singleton
    fun provideSessionDataStore(context: Context): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile(SESSION_DATA_STORE_NAME) },
    )
}
