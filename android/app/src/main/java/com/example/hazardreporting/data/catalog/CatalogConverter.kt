package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.data.catalog.dto.CategoryDto
import com.example.hazardreporting.data.catalog.dto.SeverityDto
import com.example.hazardreporting.data.catalog.dto.SiteDto
import com.example.hazardreporting.data.catalog.entity.CategoryEntity
import com.example.hazardreporting.data.catalog.entity.SeverityEntity
import com.example.hazardreporting.data.catalog.entity.SiteEntity
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import javax.inject.Inject

/**
 * Converts the catalog from the server's DTOs to Room entities, and from entities to domain models
 *
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogConverter @Inject constructor() {

    /**
     * Converts a server category at [position] in the server's list to a Room entity
     */
    fun convert(dto: CategoryDto, position: Int): CategoryEntity = CategoryEntity(
        id = dto.id,
        name = dto.name,
        position = position,
    )

    /**
     * Converts a server severity to a Room entity
     */
    fun convert(dto: SeverityDto): SeverityEntity = SeverityEntity(
        id = dto.id,
        name = dto.name,
        rank = dto.rank,
    )

    /**
     * Converts a server site at [position] in the server's list to a Room entity
     */
    fun convert(dto: SiteDto, position: Int): SiteEntity = SiteEntity(
        id = dto.id,
        name = dto.name,
        lat = dto.lat,
        lng = dto.lng,
        position = position,
    )

    /**
     * Converts a stored category to a domain [Category]
     */
    fun convert(entity: CategoryEntity): Category = Category(
        id = entity.id,
        name = entity.name,
    )

    /**
     * Converts a stored severity to a domain [Severity]
     */
    fun convert(entity: SeverityEntity): Severity = Severity(
        id = entity.id,
        name = entity.name,
        rank = entity.rank,
    )

    /**
     * Converts a stored site to a domain [Site]
     */
    fun convert(entity: SiteEntity): Site = Site(
        id = entity.id,
        name = entity.name,
        lat = entity.lat,
        lng = entity.lng,
    )
}
