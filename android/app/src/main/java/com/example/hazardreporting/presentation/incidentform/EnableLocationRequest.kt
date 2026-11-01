package com.example.hazardreporting.presentation.incidentform

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * @author Roman Pliskin on 29.10.2026
 */

/**
 * Remembers a request to turn location on. Calling it shows Google Play services' "Turn on location" dialog and
 * reports through [onResult] whether location is on afterwards. Location that is already on, e.g. turned on
 * meanwhile, is reported at once, without a dialog. A device that can't show the dialog, and a screen that leaves
 * before the check answers, report it as off, so the form never waits forever
 *
 * @param onResult called with whether location is on
 */
@Composable
fun rememberEnableLocationRequest(onResult: (isEnabled: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        currentOnResult(result.resultCode == Activity.RESULT_OK)
    }

    return remember(context, coroutineScope, launcher) {
        {
            // Scoped to the screen, so the dialog is never launched through a launcher the screen already dropped
            coroutineScope.launch {
                var isAnswered = false
                try {
                    when (val check = checkLocationSettings(context)) {
                        LocationSettingsCheck.Satisfied -> currentOnResult(true)
                        LocationSettingsCheck.Unresolvable -> currentOnResult(false)
                        is LocationSettingsCheck.Resolvable -> launcher.launch(check.request)
                    }
                    isAnswered = true
                } finally {
                    if (!isAnswered) currentOnResult(false)
                }
            }
        }
    }
}

private suspend fun checkLocationSettings(context: Context): LocationSettingsCheck {
    val settingsRequest = LocationSettingsRequest.Builder()
        .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MILLIS).build())
        // Shows the dialog even if the technician declined it before
        .setAlwaysShow(true)
        .build()
    return suspendCancellableCoroutine { continuation ->
        LocationServices.getSettingsClient(context).checkLocationSettings(settingsRequest)
            .addOnSuccessListener { continuation.resume(LocationSettingsCheck.Satisfied) }
            .addOnFailureListener { exception ->
                val check = if (exception is ResolvableApiException) {
                    LocationSettingsCheck.Resolvable(IntentSenderRequest.Builder(exception.resolution).build())
                } else {
                    LocationSettingsCheck.Unresolvable
                }
                continuation.resume(check)
            }
    }
}

/**
 * Outcome of checking the device's location settings
 */
private sealed interface LocationSettingsCheck {

    /**
     * Location is on
     */
    data object Satisfied : LocationSettingsCheck

    /**
     * Location is off, and [request] shows the dialog that turns it on
     */
    data class Resolvable(val request: IntentSenderRequest) : LocationSettingsCheck

    /**
     * Location is off, and the device can't offer to turn it on
     */
    data object Unresolvable : LocationSettingsCheck
}

/**
 * Interval of the location request the settings are checked against. Only its priority matters: nothing is requested
 */
private const val LOCATION_INTERVAL_MILLIS = 10_000L
