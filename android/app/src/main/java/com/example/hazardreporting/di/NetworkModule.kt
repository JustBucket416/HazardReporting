package com.example.hazardreporting.di

import com.example.hazardreporting.BuildConfig
import com.example.hazardreporting.data.session.AuthInterceptor
import com.example.hazardreporting.data.session.SessionApi
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlinx.serialization.json.Json
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

    private const val AUTHORIZATION_HEADER = "Authorization"
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
            loggingInterceptor.redactHeader(AUTHORIZATION_HEADER)
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    /**
     * Provides Retrofit for the server at [BASE_URL]
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .baseUrl(BASE_URL)
        .build()

    /**
     * Provides the Retrofit interface for the session routes
     */
    @Provides
    @Singleton
    fun provideSessionApi(retrofit: Retrofit): SessionApi = retrofit.create()
}
