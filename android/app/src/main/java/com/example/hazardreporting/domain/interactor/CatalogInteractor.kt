package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of the catalog: categories, severities and sites
 *
 * @property catalogRepository stores the catalog
 *
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogInteractor(
    private val catalogRepository: CatalogRepository,
) {

    /**
     * Observes the categories, in the server's order
     */
    fun observeCategories(): Flow<List<Category>> = catalogRepository.observeCategories()

    /**
     * Observes the severities, from Low to Critical
     */
    fun observeSeverities(): Flow<List<Severity>> = catalogRepository.observeSeverities()

    /**
     * Observes the sites, in the server's order
     */
    fun observeSites(): Flow<List<Site>> = catalogRepository.observeSites()
}
