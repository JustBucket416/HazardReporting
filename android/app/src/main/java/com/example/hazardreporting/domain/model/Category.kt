package com.example.hazardreporting.domain.model

/**
 * The kind of problem an incident describes, e.g. Electrical or Plumbing
 *
 * @property id server-generated category id
 * @property name display name
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class Category(
    val id: String,
    val name: String,
)
