package com.example.hazardreporting.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Provides Coil's [ImageLoader], which loads photos through the app's [OkHttpClient]
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object ImageLoaderModule {

    /**
     * Provides the app's [ImageLoader]. It fetches remote photos with the app's [OkHttpClient], which is created
     * lazily on Coil's background thread when the first remote photo loads
     */
    @Provides
    @Singleton
    fun provideImageLoader(context: Context, okHttpClient: Lazy<OkHttpClient>): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() })) }
            .build()
}
