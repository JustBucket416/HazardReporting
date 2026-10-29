package com.example.hazardreporting.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import javax.inject.Provider
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class ViewModelFactoryTest {

    private val boundViewModel = BoundViewModel()
    private val savedStateHandle = SavedStateHandle()
    private val viewModelComponent = mockk<ViewModelComponent>()
    private val viewModelComponentFactory = mockk<ViewModelComponent.Factory>()

    private val factory = ViewModelFactory(viewModelComponentFactory)

    @Test
    fun testCreate_BoundViewModel() {
        every { viewModelComponentFactory.create(savedStateHandle) } returns viewModelComponent
        every { viewModelComponent.viewModels() } returns mapOf(
            BoundViewModel::class.java to Provider<ViewModel> { boundViewModel }
        )

        val viewModel = factory.create(BoundViewModel::class.java, savedStateHandle)

        // Each ViewModel gets a subcomponent bound to its own saved state
        verifySequence {
            viewModelComponentFactory.create(savedStateHandle)
            viewModelComponent.viewModels()
        }

        Truth.assertThat(viewModel).isSameInstanceAs(boundViewModel)
    }

    @Test
    fun testCreate_UnboundViewModel() {
        every { viewModelComponentFactory.create(savedStateHandle) } returns viewModelComponent
        every { viewModelComponent.viewModels() } returns mapOf(
            BoundViewModel::class.java to Provider<ViewModel> { boundViewModel }
        )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnboundViewModel::class.java, savedStateHandle)
        }
    }

    private class BoundViewModel : ViewModel()

    private class UnboundViewModel : ViewModel()
}
