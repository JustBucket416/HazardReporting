package com.example.hazardreporting.domain.model

/**
 * The fields a technician fills in on the report form. Unlike a stored incident, any of them may still be
 * missing or invalid; [com.example.hazardreporting.domain.interactor.IncidentInteractor] validates them
 *
 * @property title short description of the problem
 * @property description what the technician saw; may be empty
 * @property categoryId id of the chosen category, or null if none is chosen yet
 * @property severityId id of the chosen severity, or null if none is chosen yet
 * @property siteId id of the chosen site, or null if none is chosen yet
 * @property locationDescription where on the site the problem is; may be empty
 * @property position the device's position, or null if it isn't available
 * @property photos the attached photos, in order
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentDraft(
    val title: String,
    val description: String,
    val categoryId: String?,
    val severityId: String?,
    val siteId: String?,
    val locationDescription: String,
    val position: Position?,
    val photos: List<Photo>,
) {

    /**
     * A field of the draft that validation can reject
     */
    enum class Field {

        /**
         * The title: 1 to [MAX_TITLE_LENGTH] characters after trimming
         */
        Title,

        /**
         * The description: at most [MAX_DESCRIPTION_LENGTH] characters after trimming
         */
        Description,

        /**
         * The category: must be chosen
         */
        Category,

        /**
         * The severity: must be chosen
         */
        Severity,

        /**
         * The site: must be chosen
         */
        Site,

        /**
         * The photos: [MIN_PHOTOS] to [MAX_PHOTOS] of them
         */
        Photos,
    }

    /**
     * Limits of the draft's fields, shared by validation and the form's hints
     */
    companion object {

        /**
         * Most characters a title may have after trimming
         */
        const val MAX_TITLE_LENGTH = 80

        /**
         * Most characters a description may have after trimming
         */
        const val MAX_DESCRIPTION_LENGTH = 1000

        /**
         * Fewest photos an incident may have
         */
        const val MIN_PHOTOS = 1

        /**
         * Most photos an incident may have
         */
        const val MAX_PHOTOS = 3
    }
}
