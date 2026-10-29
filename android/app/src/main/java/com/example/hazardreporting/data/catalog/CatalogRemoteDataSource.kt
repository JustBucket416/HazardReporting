package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.data.catalog.dto.CategoryDto
import com.example.hazardreporting.data.catalog.dto.SeverityDto
import com.example.hazardreporting.data.catalog.dto.SiteDto
import com.example.hazardreporting.data.remote.RemoteErrorInterceptor
import javax.inject.Inject

/**
 * Downloads the catalog. A missing connection fails with
 * [com.example.hazardreporting.domain.exception.NoConnectionException] and a rejected session with
 * [com.example.hazardreporting.domain.exception.UnauthorizedException], both thrown by [RemoteErrorInterceptor]
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
    suspend fun fetchCategories(): List<CategoryDto> = api.getCategories()

    /**
     * Downloads every severity
     */
    suspend fun fetchSeverities(): List<SeverityDto> = api.getSeverities()

    /**
     * Downloads every site
     */
    suspend fun fetchSites(): List<SiteDto> = api.getSites()
}
