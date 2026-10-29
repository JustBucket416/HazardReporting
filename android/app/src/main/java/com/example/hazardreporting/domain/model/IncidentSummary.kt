package com.example.hazardreporting.domain.model

/**
 * The short form of an incident that the technician sees in their list
 *
 * @property localId id of the incident on the device; screens address incidents by it only
 * @property serverId id of the incident on the server, or null until the server accepts the report
 * @property title short description of the problem
 * @property categoryId id of the incident's [Category]
 * @property severityId id of the incident's [Severity]
 * @property siteId id of the [Site] the incident belongs to
 * @property status where the incident stands in its lifecycle
 * @property createdAt when the technician first saved the report, in epoch milliseconds; never changes
 * @property syncState whether the incident has a pending change
 * @property hasStatusChangeNotice whether a pending change was refused because an operator had already
 * moved the incident out of Open, and the technician hasn't dismissed the notice yet
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentSummary(
    val localId: Long,
    val serverId: String?,
    val title: String,
    val categoryId: String,
    val severityId: String,
    val siteId: String,
    val status: IncidentStatus,
    val createdAt: Long,
    val syncState: SyncState,
    val hasStatusChangeNotice: Boolean,
)
