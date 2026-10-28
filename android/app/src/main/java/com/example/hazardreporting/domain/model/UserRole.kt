package com.example.hazardreporting.domain.model

/**
 * What a [User] does in the organisation
 *
 * @author Roman Pliskin on 29.10.2026
 */
enum class UserRole {

    /**
     * Reports incidents from the field
     */
    Technician,

    /**
     * Triages and resolves incidents from the dashboard
     */
    Operator,

    /**
     * A role this app version doesn't know
     */
    Unknown,
}
