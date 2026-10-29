package com.example.hazardreporting.di

import com.example.hazardreporting.BuildConfig
import com.example.hazardreporting.data.catalog.CatalogApi
import com.example.hazardreporting.data.incident.IncidentApi
import com.example.hazardreporting.data.remote.BaseUrl
import com.example.hazardreporting.data.session.AuthInterceptor
import com.example.hazardreporting.data.session.SessionApi
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/**
 * Base URL of the local server, as seen from the Android emulator
 */
const val BASE_URL = "http://10.0.2.2:3001/"

/**
 * Provides the network stack: JSON, OkHttp, Retrofit and the Retrofit interfaces
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object NetworkModule {

    private const val JSON_MEDIA_TYPE = "application/json"

    /**
     * Provides the JSON format for the server's bodies. Unknown keys are ignored,
     * so the server can add fields without breaking the app
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /**
     * Provides the OkHttp client. It authorises requests with [authInterceptor] and, in debug builds only,
     * logs them with the token redacted
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            loggingInterceptor.redactHeader(SessionApi.AUTHORIZATION_HEADER)
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    /**
     * Provides [BASE_URL] as the [BaseUrl] that Retrofit and the data layer's converters use
     */
    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): HttpUrl = BASE_URL.toHttpUrl()

    /**
     * Provides Retrofit for the server at [baseUrl]
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json, @BaseUrl baseUrl: HttpUrl): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .baseUrl(baseUrl)
            .build()

    /**
     * Provides the Retrofit interface for the session routes
     */
    @Provides
    @Singleton
    fun provideSessionApi(retrofit: Retrofit): SessionApi = retrofit.create()

    /**
     * Provides the Retrofit interface for the catalog routes
     */
    @Provides
    @Singleton
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create()

    /**
     * Provides the Retrofit interface for the technician incident routes
     */
    @Provides
    @Singleton
    fun provideIncidentApi(retrofit: Retrofit): IncidentApi = retrofit.create()
}
