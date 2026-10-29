package com.example.hazardreporting.domain.model

/**
 * A picture of the problem attached to an incident: a file on the device until it is uploaded,
 * then a URL on the server
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface Photo {

    /**
     * A photo stored in the app's private files, not uploaded yet
     *
     * @property path absolute path of the file
     */
    data class Local(val path: String) : Photo

    /**
     * A photo on the server
     *
     * @property url absolute URL of the photo
     */
    data class Remote(val url: String) : Photo
}
