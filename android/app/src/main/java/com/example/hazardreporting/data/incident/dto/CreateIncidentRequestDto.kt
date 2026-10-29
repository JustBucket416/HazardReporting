package com.example.hazardreporting.data.incident.dto

import kotlinx.serialization.Serializable

/**
 * Body of `POST /incidents`, which reports an incident. The server recognises a retried report by the reporter
 * and [createdAt] (ADR-0003). [position] is sent even when it is null
 *
 * @property title short description of the problem
 * @property description what the technician saw; may be empty
 * @property categoryId id of the incident's category
 * @property severityId id of the incident's severity
 * @property siteId id of the site the incident belongs to
 * @property locationDescription where on the site the problem is; may be empty
 * @property position the device's position when the incident was reported, or null
 * @property photoUrls one to three uploaded photo URLs, relative to the server, in order
 * @property createdAt when the technician first saved the report, in epoch milliseconds; never changes
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class CreateIncidentRequestDto(
    val title: String,
    val description: String,
    val categoryId: String,
    val severityId: String,
    val siteId: String,
    val locationDescription: String,
    val position: PositionDto?,
    val photoUrls: List<String>,
    val createdAt: Long,
)
