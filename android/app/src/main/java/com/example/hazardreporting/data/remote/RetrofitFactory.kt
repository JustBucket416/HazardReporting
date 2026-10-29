package com.example.hazardreporting.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * @author Roman Pliskin on 29.10.2026
 */

/**
 * Base URL of the local server, as seen from the Android emulator. Converters also resolve the server's
 * relative URLs (e.g. `/uploads/ab12.jpg`) against it
 */
const val BASE_URL = "http://10.0.2.2:3001/"

private const val JSON_MEDIA_TYPE = "application/json"

/**
 * Creates Retrofit for the server at [BASE_URL]
 *
 * @param okHttpClient used for making the requests
 * @param json used for de/serializing the bodies
 */
fun createRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
    .client(okHttpClient)
    .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
    .baseUrl(BASE_URL)
    .build()
