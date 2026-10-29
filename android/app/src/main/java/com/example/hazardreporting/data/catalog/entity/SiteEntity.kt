package com.example.hazardreporting.data.catalog.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row of the `sites` table
 *
 * @property id server-generated site id
 * @property name display name
 * @property lat latitude of the site
 * @property lng longitude of the site
 * @property position index in the server's list, which keeps the server's order
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val position: Int,
)
