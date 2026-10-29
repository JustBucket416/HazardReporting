package com.example.hazardreporting.presentation.incidentform

import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Everything the report form must keep across process death. [IncidentFormViewModel] keeps it in its
 * `SavedStateHandle` as JSON ([encode]), so a half-filled form and a photo the camera is taking survive
 *
 * @property fields what the technician has entered
 * @property initialFields the fields the form opened with, which tell whether there are unsaved changes
 * @property position the captured position, or null if there is none
 * @property positionStatus where capturing the position stands
 * @property touchedFields fields that lost focus once, whose errors show before the first Save
 * @property isSaveAttempted whether Save was tapped once, after which every error shows
 * @property addedPhotoPaths files this form created, which are deleted if the form is discarded
 * @property pendingCameraPhotoPath file the camera is writing to, or null when no picture is being taken
 * @property isPrefilled whether an edit form holds the stored incident yet; a new report's form never needs to
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class IncidentFormState(
    val fields: IncidentFormFields = IncidentFormFields(),
    val initialFields: IncidentFormFields = IncidentFormFields(),
    val position: FormPosition? = null,
    val positionStatus: PositionStatus = PositionStatus.Locating,
    val touchedFields: Set<IncidentDraft.Field> = emptySet(),
    val isSaveAttempted: Boolean = false,
    val addedPhotoPaths: List<String> = emptyList(),
    val pendingCameraPhotoPath: String? = null,
    val isPrefilled: Boolean = false,
) {

    /**
     * Whether the technician changed anything since the form opened
     */
    val hasUnsavedChanges: Boolean
        get() = fields != initialFields

    /**
     * Returns the draft the form describes
     */
    fun toDraft(): IncidentDraft = IncidentDraft(
        title = fields.title,
        description = fields.description,
        categoryId = fields.categoryId,
        severityId = fields.severityId,
        siteId = fields.siteId,
        locationDescription = fields.locationDescription,
        position = position?.toPosition(),
        photos = fields.photos.map { it.toPhoto() },
    )

    /**
     * Returns the state as JSON, a form [SavedStateHandle][androidx.lifecycle.SavedStateHandle] can keep
     */
    fun encode(): String = json.encodeToString(serializer(), this)

    /**
     * Decodes saved form states
     */
    companion object {

        // Tolerates states saved by an older version of the form
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Returns the state that [encoded] holds, or null if it holds none
         */
        fun decodeOrNull(encoded: String?): IncidentFormState? {
            if (encoded.isNullOrEmpty()) return null
            return try {
                json.decodeFromString(serializer(), encoded)
            } catch (e: SerializationException) {
                // A state this version can't read starts the form afresh rather than crashing it
                null
            }
        }
    }
}

/**
 * The fields of the report form the technician fills in
 *
 * @property title text of the title field
 * @property description text of the description field
 * @property categoryId id of the chosen category, or null
 * @property severityId id of the chosen severity, or null
 * @property siteId id of the chosen site, or null
 * @property locationDescription text of the location description field
 * @property photos the attached photos, in order
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class IncidentFormFields(
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val severityId: String? = null,
    val siteId: String? = null,
    val locationDescription: String = "",
    val photos: List<FormPhoto> = emptyList(),
)

/**
 * A photo attached on the form, in a form that can be saved; mirrors [Photo]
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
sealed interface FormPhoto {

    /**
     * Returns the domain photo
     */
    fun toPhoto(): Photo

    /**
     * A photo file on the device
     *
     * @property path absolute path of the file
     */
    @Serializable
    @SerialName("local")
    data class Local(val path: String) : FormPhoto {
        override fun toPhoto(): Photo = Photo.Local(path)
    }

    /**
     * A photo on the server, which an incident being edited already has
     *
     * @property url absolute URL of the photo
     */
    @Serializable
    @SerialName("remote")
    data class Remote(val url: String) : FormPhoto {
        override fun toPhoto(): Photo = Photo.Remote(url)
    }

    /**
     * Converts domain photos to form photos
     */
    companion object {

        /**
         * Returns the form photo of [photo]
         */
        fun of(photo: Photo): FormPhoto = when (photo) {
            is Photo.Local -> Local(photo.path)
            is Photo.Remote -> Remote(photo.url)
        }
    }
}

/**
 * A captured position in a form that can be saved; mirrors [Position]
 *
 * @property lat latitude in degrees
 * @property lng longitude in degrees
 * @property accuracyM accuracy in metres
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class FormPosition(
    val lat: Double,
    val lng: Double,
    val accuracyM: Double,
) {

    /**
     * Returns the domain position
     */
    fun toPosition(): Position = Position(lat, lng, accuracyM)

    /**
     * Converts domain positions to form positions
     */
    companion object {

        /**
         * Returns the form position of [position]
         */
        fun of(position: Position): FormPosition = FormPosition(position.lat, position.lng, position.accuracyM)
    }
}

/**
 * Where capturing the report's position stands
 *
 * @author Roman Pliskin on 29.10.2026
 */
enum class PositionStatus {

    /**
     * Permission is being asked for, or a fix is being waited for
     */
    Locating,

    /**
     * A position was captured
     */
    Captured,

    /**
     * Permission was denied or no fix was available; the report is saved without a position
     */
    Unavailable,
}
