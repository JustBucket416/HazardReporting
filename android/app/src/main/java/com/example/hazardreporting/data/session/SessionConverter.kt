package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.example.hazardreporting.data.session.dto.UserDto
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import javax.inject.Inject

/**
 * Converts the server's session DTOs to [Session] and the domain [User]
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SessionConverter @Inject constructor() {

    /**
     * Converts a login response to the [Session] it starts
     */
    fun convert(dto: LoginResponseDto): Session = Session(dto.token, convert(dto.user))

    /**
     * Converts a server user to a domain [User]. Unknown roles become [UserRole.Unknown]
     */
    fun convert(dto: UserDto): User = User(
        id = dto.id,
        email = dto.email,
        name = dto.name,
        role = when (dto.role) {
            ROLE_TECHNICIAN -> UserRole.Technician
            ROLE_OPERATOR -> UserRole.Operator
            else -> UserRole.Unknown
        },
    )

    private companion object {
        const val ROLE_TECHNICIAN = "technician"
        const val ROLE_OPERATOR = "operator"
    }
}
