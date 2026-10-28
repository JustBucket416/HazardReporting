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
import com.google.common.truth.Truth
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogConverterTest {

    private val converter = CatalogConverter()

    @Test
    fun testConvertCategoryDto() {
        val entity = converter.convert(CategoryDto("c1", "Electrical"), position = 2)

        Truth.assertThat(entity).isEqualTo(CategoryEntity("c1", "Electrical", position = 2))
    }

    @Test
    fun testConvertSeverityDto() {
        val entity = converter.convert(SeverityDto("s4", "Critical", rank = 4))

        Truth.assertThat(entity).isEqualTo(SeverityEntity("s4", "Critical", rank = 4))
    }

    @Test
    fun testConvertSiteDto() {
        val entity = converter.convert(SiteDto("site1", "North Plant", LAT, LNG), position = 1)

        Truth.assertThat(entity).isEqualTo(SiteEntity("site1", "North Plant", LAT, LNG, position = 1))
    }

    @Test
    fun testConvertCategoryEntity() {
        val category = converter.convert(CategoryEntity("c1", "Electrical", position = 2))

        Truth.assertThat(category).isEqualTo(Category("c1", "Electrical"))
    }

    @Test
    fun testConvertSeverityEntity() {
        val severity = converter.convert(SeverityEntity("s4", "Critical", rank = 4))

        Truth.assertThat(severity).isEqualTo(Severity("s4", "Critical", rank = 4))
    }

    @Test
    fun testConvertSiteEntity() {
        val site = converter.convert(SiteEntity("site1", "North Plant", LAT, LNG, position = 1))

        Truth.assertThat(site).isEqualTo(Site("site1", "North Plant", LAT, LNG))
    }

    private companion object {
        const val LAT = 59.9343
        const val LNG = 30.3351
    }
}
