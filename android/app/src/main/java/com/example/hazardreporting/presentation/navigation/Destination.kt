package com.example.hazardreporting.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * A type-safe navigation route. Incidents are addressed by their local id only (ADR-0002)
 *
 * @author Roman Pliskin on 29.10.2026
 */
sealed interface Destination

/**
 * The sign-in screen
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data object Login : Destination

/**
 * The list of the technician's incidents
 *
 * @property refreshOnOpen whether the list pulls the technician's incidents as soon as it opens, which signing in
 * asks for; otherwise it shows what is stored, and pulls only on pull-to-refresh (ADR-0002)
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class IncidentList(val refreshOnOpen: Boolean = false) : Destination

/**
 * One incident's details
 *
 * @property localId local id of the incident
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class IncidentDetail(val localId: Long) : Destination

/**
 * The form that reports a new incident or edits an existing one
 *
 * @property localId local id of the incident to edit, or null to report a new one
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class IncidentForm(val localId: Long? = null) : Destination
