package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.example.hazardreporting.data.session.dto.UserDto
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.google.common.truth.Truth
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class SessionConverterTest {

    private val converter = SessionConverter()

    @Test
    fun testConvertLoginResponse() {
        val dto = LoginResponseDto(TOKEN, userDto(role = "technician"))

        val session = converter.convert(dto)

        Truth.assertThat(session).isEqualTo(Session(TOKEN, user(UserRole.Technician)))
    }

    @Test
    fun testConvertUser_Technician() {
        val user = converter.convert(userDto(role = "technician"))

        Truth.assertThat(user).isEqualTo(user(UserRole.Technician))
    }

    @Test
    fun testConvertUser_Operator() {
        val user = converter.convert(userDto(role = "operator"))

        Truth.assertThat(user).isEqualTo(user(UserRole.Operator))
    }

    @Test
    fun testConvertUser_UnknownRole() {
        val user = converter.convert(userDto(role = "auditor"))

        Truth.assertThat(user).isEqualTo(user(UserRole.Unknown))
    }

    private fun userDto(role: String) = UserDto(USER_ID, EMAIL, NAME, role)

    private fun user(role: UserRole) = User(USER_ID, EMAIL, NAME, role)

    private companion object {
        const val TOKEN = "token"
        const val USER_ID = "u1"
        const val EMAIL = "tech1@example.com"
        const val NAME = "Tech One"
    }
}
