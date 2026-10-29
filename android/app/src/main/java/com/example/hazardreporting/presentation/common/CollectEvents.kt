package com.example.hazardreporting.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects a ViewModel's one-off [events] while the screen is at least started, and passes each to [onEvent].
 * Events sent while the screen is stopped wait in the ViewModel's channel until it starts again
 *
 * @param events the ViewModel's event flow
 * @param onEvent handles one event
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun <T> CollectEvents(events: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(events, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect { currentOnEvent(it) }
        }
    }
}
