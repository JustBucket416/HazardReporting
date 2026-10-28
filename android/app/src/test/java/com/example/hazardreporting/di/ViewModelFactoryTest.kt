package com.example.hazardreporting.di

import androidx.lifecycle.ViewModel
import com.google.common.truth.Truth
import javax.inject.Provider
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class ViewModelFactoryTest {

    private val boundViewModel = BoundViewModel()

    private val factory = ViewModelFactory(
        mapOf(BoundViewModel::class.java to Provider<ViewModel> { boundViewModel })
    )

    @Test
    fun testCreate_BoundViewModel() {
        val viewModel = factory.create(BoundViewModel::class.java)

        Truth.assertThat(viewModel).isSameInstanceAs(boundViewModel)
    }

    @Test
    fun testCreate_UnboundViewModel() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnboundViewModel::class.java)
        }
    }

    private class BoundViewModel : ViewModel()

    private class UnboundViewModel : ViewModel()
}
