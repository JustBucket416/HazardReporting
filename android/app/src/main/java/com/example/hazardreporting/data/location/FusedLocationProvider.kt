package com.example.hazardreporting.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.repository.LocationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [LocationProvider] backed by the fused location provider. It asks for a fresh high-accuracy fix and waits
 * up to [CURRENT_LOCATION_TIMEOUT_MILLIS] for it; if none arrives, it falls back to the last known fix,
 * but only if that is younger than [LAST_LOCATION_MAX_AGE_NANOS]. Either fine or coarse (approximate)
 * permission is enough
 *
 * @property context used for checking the location permissions
 * @property client the fused location provider
 * @property locationManager used for checking whether location is turned on
 *
 * @author Roman Pliskin on 29.10.2026
 */
class FusedLocationProvider @Inject constructor(
    private val context: Context,
    private val client: FusedLocationProviderClient,
    private val locationManager: LocationManager,
) : LocationProvider {

    override suspend fun currentPosition(): Position? {
        if (!hasLocationPermission()) return null
        val location = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MILLIS) { currentLocation() }
            ?: lastLocation()?.takeIf { it.isFresh() }
        return location?.let { Position(it.latitude, it.longitude, it.accuracy.toDouble()) }
    }

    override fun isLocationEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)

    private fun hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun currentLocation(): Location? {
        val cancellation = CancellationTokenSource()
        return try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .awaitOrNull { cancellation.cancel() }
        } catch (e: SecurityException) {
            // The permission was revoked after it was checked
            null
        }
    }

    private suspend fun lastLocation(): Location? = try {
        client.lastLocation.awaitOrNull()
    } catch (e: SecurityException) {
        // The permission was revoked after it was checked
        null
    }

    private fun Location.isFresh(): Boolean =
        SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos < LAST_LOCATION_MAX_AGE_NANOS

    /**
     * Suspends until the task completes and returns its result, or null if it fails.
     * [onCancel] runs if the waiting coroutine is cancelled, e.g. by the timeout
     */
    private suspend fun Task<Location>.awaitOrNull(onCancel: () -> Unit = {}): Location? =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resume(null) }
            continuation.invokeOnCancellation { onCancel() }
        }

    private companion object {
        const val CURRENT_LOCATION_TIMEOUT_MILLIS = 10_000L
        const val LAST_LOCATION_MAX_AGE_NANOS = 2 * 60 * 1_000_000_000L
        val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
