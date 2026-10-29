package com.example.hazardreporting.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.hazardreporting.domain.model.Position
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class FusedLocationProviderTest {

    private val context = mockk<Context>()
    private val client = mockk<FusedLocationProviderClient>()

    private val provider = FusedLocationProvider(context, client)

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class, SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returns NOW_NANOS
        grant(fine = true, coarse = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class, SystemClock::class)
    }

    @Test
    fun testCurrentPosition_CurrentFix() = runTest {
        every { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>()) } returns
            succeededTask(location(53.5, -2.2, accuracy = 12f, ageNanos = 0))

        Truth.assertThat(provider.currentPosition()).isEqualTo(Position(53.5, -2.2, 12.0))
    }

    @Test
    fun testCurrentPosition_ApproximateOnly() = runTest {
        grant(fine = false, coarse = true)
        every { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>()) } returns
            succeededTask(location(53.5, -2.2, accuracy = 2000f, ageNanos = 0))

        // Approximate location is accepted
        Truth.assertThat(provider.currentPosition()).isEqualTo(Position(53.5, -2.2, 2000.0))
    }

    @Test
    fun testCurrentPosition_NoPermission() = runTest {
        grant(fine = false, coarse = false)

        Truth.assertThat(provider.currentPosition()).isNull()

        verify { client wasNot Called }
    }

    @Test
    fun testCurrentPosition_NoCurrentFixFreshLastFix() = runTest {
        every { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>()) } returns
            succeededTask(null)
        every { client.lastLocation } returns succeededTask(location(1.0, 2.0, accuracy = 30f, ageNanos = FRESH_AGE))

        Truth.assertThat(provider.currentPosition()).isEqualTo(Position(1.0, 2.0, 30.0))
    }

    @Test
    fun testCurrentPosition_CurrentFixFailsStaleLastFix() = runTest {
        every { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>()) } returns
            failedTask()
        every { client.lastLocation } returns succeededTask(location(1.0, 2.0, accuracy = 30f, ageNanos = STALE_AGE))

        // A fix two minutes old or more isn't used
        Truth.assertThat(provider.currentPosition()).isNull()
    }

    @Test
    fun testCurrentPosition_TimeoutFreshLastFix() = runTest {
        every { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>()) } returns
            pendingTask()
        every { client.lastLocation } returns succeededTask(location(1.0, 2.0, accuracy = 30f, ageNanos = FRESH_AGE))

        // The current fix is given up after 10 s of (virtual) time
        Truth.assertThat(provider.currentPosition()).isEqualTo(Position(1.0, 2.0, 30.0))
        Truth.assertThat(testScheduler.currentTime).isEqualTo(10_000L)
    }

    @Test
    fun testCurrentPosition_NothingAvailable() = runTest {
        every { client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>()) } returns
            succeededTask(null)
        every { client.lastLocation } returns succeededTask(null)

        Truth.assertThat(provider.currentPosition()).isNull()
    }

    @Test
    fun testCurrentPosition_PermissionRevoked() = runTest {
        every {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, any<CancellationToken>())
        } throws SecurityException()
        every { client.lastLocation } throws SecurityException()

        Truth.assertThat(provider.currentPosition()).isNull()
    }

    private fun grant(fine: Boolean, coarse: Boolean) {
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) } returns
            if (fine) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) } returns
            if (coarse) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    private fun location(lat: Double, lng: Double, accuracy: Float, ageNanos: Long): Location {
        val location = mockk<Location>()
        every { location.latitude } returns lat
        every { location.longitude } returns lng
        every { location.accuracy } returns accuracy
        every { location.elapsedRealtimeNanos } returns NOW_NANOS - ageNanos
        return location
    }

    private fun succeededTask(location: Location?): Task<Location> {
        val task = mockk<Task<Location>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<in Location>>()) } answers {
            firstArg<OnSuccessListener<Location?>>().onSuccess(location)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private fun failedTask(): Task<Location> {
        val task = mockk<Task<Location>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<in Location>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(IllegalStateException("Location unavailable"))
            task
        }
        return task
    }

    private fun pendingTask(): Task<Location> {
        val task = mockk<Task<Location>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<in Location>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        return task
    }

    private companion object {
        const val NOW_NANOS = 1_000_000_000_000L
        const val FRESH_AGE = 60 * 1_000_000_000L
        const val STALE_AGE = 2 * 60 * 1_000_000_000L
    }
}
