package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.data.catalog.dto.CategoryDto
import com.example.hazardreporting.data.catalog.dto.SeverityDto
import com.example.hazardreporting.data.catalog.dto.SiteDto
import com.example.hazardreporting.data.remote.mapRemoteErrors
import javax.inject.Inject

/**
 * Downloads the catalog and maps failures to domain exceptions: a missing connection becomes
 * [com.example.hazardreporting.domain.exception.NoConnectionException] and a rejected session
 * [com.example.hazardreporting.domain.exception.UnauthorizedException]
 *
 * @property api used for making calls to the server
 *
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogRemoteDataSource @Inject constructor(
    private val api: CatalogApi,
) {

    /**
     * Downloads every category
     */
    suspend fun fetchCategories(): List<CategoryDto> = mapRemoteErrors { api.getCategories() }

    /**
     * Downloads every severity
     */
    suspend fun fetchSeverities(): List<SeverityDto> = mapRemoteErrors { api.getSeverities() }

    /**
     * Downloads every site
     */
    suspend fun fetchSites(): List<SiteDto> = mapRemoteErrors { api.getSites() }
}
