package com.example.hazardreporting.data.incident.dto

import kotlinx.serialization.Serializable

/**
 * An item of `GET /incidents/summary`
 *
 * @property id server-generated incident id
 * @property title short description of the problem
 * @property categoryId id of the incident's category
 * @property severityId id of the incident's severity
 * @property siteId id of the site the incident belongs to
 * @property status e.g. `"open"` or `"in_progress"`
 * @property createdAt when the technician first saved the report, in epoch milliseconds
 * @property updatedAt when the server last changed the incident, in epoch milliseconds
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class IncidentSummaryDto(
    val id: String,
    val title: String,
    val categoryId: String,
    val severityId: String,
    val siteId: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)
