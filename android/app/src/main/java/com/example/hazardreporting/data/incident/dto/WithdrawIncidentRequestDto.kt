package com.example.hazardreporting.data.incident.dto

import kotlinx.serialization.Serializable

/**
 * Body of a technician's `PATCH /incidents/:id` that withdraws the incident. The server requires `status`
 * to be its only key (ADR-0005). It has no default value, so it is always encoded
 *
 * @property status always `"withdrawn"`
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Serializable
data class WithdrawIncidentRequestDto(
    val status: String,
)
