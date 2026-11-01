package com.example.hazardreporting.presentation.incidentform

/**
 * One-off events of the report form
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface IncidentFormEvent {

    /**
     * The screen asks for fine and coarse location permission and reports the result back
     */
    data object RequestLocationPermission : IncidentFormEvent

    /**
     * Location is turned off on the device; the screen asks the technician to turn it on and reports whether it is
     * on afterwards
     */
    data object RequestEnableLocation : IncidentFormEvent

    /**
     * The screen opens the camera to take a picture into the file at [path]
     *
     * @property path absolute path of the file in the app's photo storage
     */
    data class TakePhoto(val path: String) : IncidentFormEvent

    /**
     * The screen opens the photo picker
     */
    data object PickPhoto : IncidentFormEvent

    /**
     * A photo was too large to upload and wasn't attached; the screen shows a snackbar
     */
    data object PhotoTooLarge : IncidentFormEvent

    /**
     * The report was saved; the screen goes back with a "saved" result
     */
    data object Saved : IncidentFormEvent

    /**
     * The form closes without saving
     */
    data object Closed : IncidentFormEvent

    /**
     * Something nobody expected failed; the screen shows a snackbar
     */
    data object UnexpectedError : IncidentFormEvent
}
