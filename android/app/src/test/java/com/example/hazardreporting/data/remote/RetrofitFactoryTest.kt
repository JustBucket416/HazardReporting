package com.example.hazardreporting.data.remote

import com.google.common.truth.Truth
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class RetrofitFactoryTest {

    @Test
    fun testCreateRetrofit_ResolvesRelativeUrls() {
        val retrofit = createRetrofit(OkHttpClient(), Json)

        Truth.assertThat(retrofit.baseUrl().toString()).isEqualTo(BASE_URL)
        Truth.assertThat(retrofit.baseUrl().resolve("/uploads/ab12.jpg").toString())
            .isEqualTo("http://10.0.2.2:3001/uploads/ab12.jpg")
    }
}
