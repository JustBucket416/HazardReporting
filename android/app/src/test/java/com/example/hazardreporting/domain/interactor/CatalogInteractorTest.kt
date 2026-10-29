package com.example.hazardreporting.domain.interactor

import app.cash.turbine.test
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogInteractorTest {

    private val catalogRepository = mockk<CatalogRepository>()

    private val interactor = CatalogInteractor(catalogRepository)

    @Test
    fun testObserveCategories() = runTest {
        val categories = listOf(Category("c1", "Equipment"))

        every { catalogRepository.observeCategories() } returns flowOf(categories)

        interactor.observeCategories().test {
            Truth.assertThat(awaitItem()).isEqualTo(categories)
            awaitComplete()
        }

        verifySequence { catalogRepository.observeCategories() }
    }

    @Test
    fun testObserveSeverities() = runTest {
        val severities = listOf(Severity("s1", "Low", 1))

        every { catalogRepository.observeSeverities() } returns flowOf(severities)

        interactor.observeSeverities().test {
            Truth.assertThat(awaitItem()).isEqualTo(severities)
            awaitComplete()
        }

        verifySequence { catalogRepository.observeSeverities() }
    }

    @Test
    fun testObserveSites() = runTest {
        val sites = listOf(Site("site1", "North Plant", 59.9343, 30.3351))

        every { catalogRepository.observeSites() } returns flowOf(sites)

        interactor.observeSites().test {
            Truth.assertThat(awaitItem()).isEqualTo(sites)
            awaitComplete()
        }

        verifySequence { catalogRepository.observeSites() }
    }
}
