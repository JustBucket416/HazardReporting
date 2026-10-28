package com.example.hazardreporting.domain.model

/**
 * How dangerous or damaging a problem is, as judged by the technician
 *
 * @property id server-generated severity id
 * @property name display name
 * @property rank position on the scale: 1 = Low … 4 = Critical
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class Severity(
    val id: String,
    val name: String,
    val rank: Int,
)
