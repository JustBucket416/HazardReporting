package com.example.hazardreporting.domain.model

/**
 * An incident as the device knows it: always its summary, and its details once they are downloaded (ADR-0006)
 *
 * @property summary the short form shown in the list
 * @property details the rest of the incident, or null if it hasn't been downloaded yet
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class Incident(
    val summary: IncidentSummary,
    val details: IncidentDetails?,
)
