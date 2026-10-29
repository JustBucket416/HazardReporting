package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.data.catalog.dto.CategoryDto
import com.example.hazardreporting.data.catalog.dto.SeverityDto
import com.example.hazardreporting.data.catalog.dto.SiteDto
import retrofit2.http.GET

/**
 * Retrofit interface for the server's catalog routes
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface CatalogApi {

    /**
     * Returns every category
     */
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    /**
     * Returns every severity
     */
    @GET("severities")
    suspend fun getSeverities(): List<SeverityDto>

    /**
     * Returns every site
     */
    @GET("sites")
    suspend fun getSites(): List<SiteDto>
}
