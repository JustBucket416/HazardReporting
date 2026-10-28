package com.example.hazardreporting.presentation.login

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hazardreporting.R
import com.example.hazardreporting.presentation.common.CollectEvents
import com.example.hazardreporting.presentation.common.shimmer
import com.example.hazardreporting.presentation.theme.HazardReportingTheme
import kotlinx.coroutines.launch

/**
 * The sign-in screen bound to its [LoginViewModel]
 *
 * @param viewModel manages the screen's state
 * @param onLoggedIn called once signing in succeeds
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun LoginRoute(viewModel: LoginViewModel, onLoggedIn: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val unexpectedErrorMessage = stringResource(R.string.error_unexpected)

    CollectEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.LoggedIn -> onLoggedIn()
            LoginEvent.UnexpectedError -> coroutineScope.launch {
                snackbarHostState.showSnackbar(unexpectedErrorMessage)
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
    )
}

/**
 * The sign-in screen: email and password fields and a sign-in button. While a request is in flight,
 * the fields are disabled and the button shimmers but keeps its enabled look and ignores taps.
 * Errors show under the password field
 *
 * @param uiState state of the screen
 * @param snackbarHostState shows the screen's snackbars
 * @param onEmailChange called when the email text changes
 * @param onPasswordChange called when the password text changes
 * @param onSubmit called when the user taps the button or submits from the keyboard
 * @param modifier modifier for the screen's root
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        if (!uiState.isSigningIn) {
            focusManager.clearFocus()
            onSubmit()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSigningIn,
                label = { Text(stringResource(R.string.login_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSigningIn,
                label = { Text(stringResource(R.string.login_password)) },
                supportingText = if (uiState.error != null) {
                    { Text(uiState.error.message()) }
                } else {
                    null
                },
                isError = uiState.error != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                singleLine = true,
            )
            SignInButton(isSigningIn = uiState.isSigningIn, onClick = submit)
        }
    }
}

@Composable
private fun SignInButton(isSigningIn: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(ButtonDefaults.shape)
            .shimmer(
                highlightColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = SIGNING_IN_HIGHLIGHT_ALPHA),
                enabled = isSigningIn,
            ),
    ) {
        Text(stringResource(if (isSigningIn) R.string.login_signing_in else R.string.login_sign_in))
    }
}

@Composable
private fun LoginError.message(): String = stringResource(
    when (this) {
        LoginError.InvalidCredentials -> R.string.login_error_invalid_credentials
        LoginError.NoConnection -> R.string.login_error_no_connection
    }
)

private const val SIGNING_IN_HIGHLIGHT_ALPHA = 0.35f

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() {
    LoginScreenPreviewContent(LoginUiState(email = "tech1@example.com", password = "password"))
}

@Preview(name = "Signing in, light")
@Preview(name = "Signing in, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenSigningInPreview() {
    LoginScreenPreviewContent(LoginUiState(email = "tech1@example.com", password = "password", isSigningIn = true))
}

@Preview(name = "Error, light")
@Preview(name = "Error, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenErrorPreview() {
    LoginScreenPreviewContent(
        LoginUiState(email = "tech1@example.com", password = "wrong", error = LoginError.InvalidCredentials)
    )
}

@Composable
private fun LoginScreenPreviewContent(uiState: LoginUiState) {
    HazardReportingTheme {
        LoginScreen(
            uiState = uiState,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
        )
    }
}
