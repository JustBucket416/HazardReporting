package com.example.hazardreporting.data.catalog.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row of the `categories` table
 *
 * @property id server-generated category id
 * @property name display name
 * @property position index in the server's list, which keeps the server's order
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val position: Int,
)
