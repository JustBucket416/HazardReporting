package com.example.hazardreporting.di

import com.example.hazardreporting.BuildConfig
import com.example.hazardreporting.data.catalog.CatalogApi
import com.example.hazardreporting.data.incident.IncidentApi
import com.example.hazardreporting.data.remote.BASE_URL
import com.example.hazardreporting.data.remote.RemoteErrorInterceptor
import com.example.hazardreporting.data.remote.createRetrofit
import com.example.hazardreporting.data.session.AuthInterceptor
import com.example.hazardreporting.data.session.SessionApi
import com.example.hazardreporting.data.session.SessionHolder
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create

/**
 * Provides the network stack: JSON, OkHttp, Retrofit and the Retrofit interfaces
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object NetworkModule {

    /**
     * Provides the JSON format for the server's bodies. Unknown keys are ignored,
     * so the server can add fields without breaking the app
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /**
     * Provides the interceptor that turns transport failures into domain exceptions, and marks the session as
     * rejected in [sessionHolder] whenever the server answers a signed-in request with a 401
     */
    @Provides
    @Singleton
    fun provideRemoteErrorInterceptor(sessionHolder: SessionHolder): RemoteErrorInterceptor =
        RemoteErrorInterceptor(onUnauthorized = sessionHolder::markRejected)

    /**
     * Provides the OkHttp client. It maps transport failures with [remoteErrorInterceptor], authorises requests
     * with [authInterceptor] and, in debug builds only, logs them with the token redacted
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        remoteErrorInterceptor: RemoteErrorInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Before the token is added, so it can tell requests that name their own session
            .addInterceptor(remoteErrorInterceptor)
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
     * Provides Retrofit for the server at [BASE_URL]
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = createRetrofit(okHttpClient, json)

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
