package com.example.hazardreporting.di

import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.example.hazardreporting.data.session.dto.UserDto
import com.google.common.truth.Truth
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class NetworkModuleTest {

    @Test
    fun testProvideJson_IgnoresUnknownKeys() {
        val json = NetworkModule.provideJson()

        val response = json.decodeFromString<LoginResponseDto>(LOGIN_RESPONSE)

        Truth.assertThat(response).isEqualTo(
            LoginResponseDto("t1", UserDto("u1", "tech1@example.com", "Tech One", "technician"))
        )
    }

    private companion object {
        val LOGIN_RESPONSE = """
            {
              "token": "t1",
              "user": { "id": "u1", "email": "tech1@example.com", "name": "Tech One", "role": "technician", "avatar": null },
              "expiresAt": null
            }
        """.trimIndent()
    }
}
