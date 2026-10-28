package com.example.hazardreporting.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.hazardreporting.R
import com.example.hazardreporting.presentation.common.PlaceholderScreen
import com.example.hazardreporting.presentation.login.LoginRoute

/**
 * The app's navigation graph
 *
 * @param startDestination the first screen: [Login] or [IncidentList], depending on whether someone is signed in
 * @param viewModelFactory creates the screens' ViewModels
 * @param modifier modifier for the [NavHost]
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun HazardReportingNavHost(
    startDestination: Destination,
    viewModelFactory: ViewModelProvider.Factory,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Login> {
            LoginRoute(
                viewModel = viewModel(factory = viewModelFactory),
                onLoggedIn = { navController.navigateClearingBackStack(IncidentList) },
            )
        }
        composable<IncidentList> {
            PlaceholderScreen(title = stringResource(R.string.incident_list_title))
        }
        composable<IncidentDetail> {
            PlaceholderScreen(title = stringResource(R.string.incident_detail_title))
        }
        composable<IncidentForm> { backStackEntry ->
            val isEditing = backStackEntry.toRoute<IncidentForm>().localId != null
            val title = if (isEditing) R.string.incident_form_edit_title else R.string.incident_form_new_title
            PlaceholderScreen(title = stringResource(title))
        }
    }
}

/**
 * Navigates to [destination] and removes every other screen from the back stack, so Back leaves the app
 */
private fun NavController.navigateClearingBackStack(destination: Destination) {
    navigate(destination) {
        popUpTo(graph.id) { inclusive = true }
    }
}
