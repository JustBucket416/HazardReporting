package com.example.hazardreporting.data.incident.dto

import kotlinx.serialization.Serializable

/**
 * A full incident as `GET /incidents/:id` sends it to a technician, without assignee and notes
 *
 * @property id server-generated incident id
 * @property title short description of the problem
 * @property description what the technician saw; may be empty
 * @property categoryId id of the incident's category
 * @property severityId id of the incident's severity
 * @property siteId id of the site the incident belongs to
 * @property locationDescription where on the site the problem is; may be empty
 * @property position the device's position when the incident was reported, or null
 * @property photoUrls one to three photo URLs relative to the server, e.g. `"/uploads/ab12.jpg"`
 * @property status e.g. `"open"` or `"in_progress"`
 * @property priority e.g. `"urgent"`, or null until an operator sets it
 * @property createdAt when the technician first saved the report, in epoch milliseconds
 * @property updatedAt when the server last changed the incident, in epoch milliseconds
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class IncidentDto(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val severityId: String,
    val siteId: String,
    val locationDescription: String,
    val position: PositionDto?,
    val photoUrls: List<String>,
    val status: String,
    val priority: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
