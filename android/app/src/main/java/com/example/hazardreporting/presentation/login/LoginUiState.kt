package com.example.hazardreporting.presentation.login

/**
 * State of the sign-in screen
 *
 * @property email text of the email field
 * @property password text of the password field
 * @property isSigningIn whether a sign-in request is in flight
 * @property error why the last sign-in failed, shown under the password field; one about the account also marks
 * the email field
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSigningIn: Boolean = false,
    val error: LoginError? = null,
)

/**
 * Why signing in failed
 *
 * @property isAboutAccount whether the entered account is at fault, rather than the connection
 *
 * @author Roman Pliskin on 29.10.2026
 */
enum class LoginError(val isAboutAccount: Boolean) {

    /**
     * The server rejected the email or password
     */
    InvalidCredentials(isAboutAccount = true),

    /**
     * The server couldn't be reached
     */
    NoConnection(isAboutAccount = false),

    /**
     * The credentials belong to someone who isn't a technician
     */
    NotTechnician(isAboutAccount = true),
}
