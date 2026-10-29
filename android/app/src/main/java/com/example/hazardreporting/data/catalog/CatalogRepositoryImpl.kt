package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of [CatalogRepository]. Room is the source of truth; [refresh] replaces it with the server's catalog
 *
 * @property remoteDataSource used for downloading the catalog
 * @property localDataSource used for storing the catalog
 * @property converter used for converting between DTOs, entities and domain models
 * @property dispatchers used for running on the IO dispatcher
 *
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogRepositoryImpl @Inject constructor(
    private val remoteDataSource: CatalogRemoteDataSource,
    private val localDataSource: CatalogLocalDataSource,
    private val converter: CatalogConverter,
    private val dispatchers: CoroutineDispatchers,
) : CatalogRepository {

    override suspend fun refresh() = withContext(dispatchers.io) {
        // Downloads everything first, so a failed download leaves the stored catalog untouched
        val categories = remoteDataSource.fetchCategories()
        val severities = remoteDataSource.fetchSeverities()
        val sites = remoteDataSource.fetchSites()

        localDataSource.replaceCategories(categories.mapIndexed { position, dto -> converter.convert(dto, position) })
        localDataSource.replaceSeverities(severities.map { converter.convert(it) })
        localDataSource.replaceSites(sites.mapIndexed { position, dto -> converter.convert(dto, position) })
    }

    override fun observeCategories(): Flow<List<Category>> = localDataSource.observeCategories()
        .map { entities -> entities.map { converter.convert(it) } }
        .flowOn(dispatchers.io)

    override fun observeSeverities(): Flow<List<Severity>> = localDataSource.observeSeverities()
        .map { entities -> entities.map { converter.convert(it) } }
        .flowOn(dispatchers.io)

    override fun observeSites(): Flow<List<Site>> = localDataSource.observeSites()
        .map { entities -> entities.map { converter.convert(it) } }
        .flowOn(dispatchers.io)
}
