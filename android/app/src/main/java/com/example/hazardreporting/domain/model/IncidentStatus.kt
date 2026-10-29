package com.example.hazardreporting.domain.model

/**
 * Where an incident stands in its lifecycle
 *
 * @author Roman Pliskin on 29.10.2026
 */
enum class IncidentStatus {

    /**
     * Reported and not yet looked at by an operator; the only status in which the reporter may edit or withdraw it
     */
    Open,

    /**
     * An operator accepted the incident as actionable, but work hasn't started
     */
    Acknowledged,

    /**
     * Work to fix the problem is under way
     */
    InProgress,

    /**
     * The problem has been fixed; final
     */
    Resolved,

    /**
     * An operator decided the incident needs no action; final
     */
    Rejected,

    /**
     * The reporter took the incident back while it was Open; final
     */
    Withdrawn,

    /**
     * A status this app version doesn't know
     */
    Unknown,
}
