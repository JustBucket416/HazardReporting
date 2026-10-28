package com.example.hazardreporting.data.catalog.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row of the `severities` table
 *
 * @property id server-generated severity id
 * @property name display name
 * @property rank position on the scale: 1 = Low … 4 = Critical
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Entity(tableName = "severities")
data class SeverityEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val rank: Int,
)
