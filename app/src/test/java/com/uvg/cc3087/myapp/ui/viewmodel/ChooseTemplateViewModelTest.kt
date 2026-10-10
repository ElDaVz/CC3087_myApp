package com.uvg.cc3087.myapp.ui.viewmodel

import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.testing.FakeTemplateRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChooseTemplateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val templates = listOf(
        FormTemplate("order-form", TemplateType.ORDER_FORM),
        FormTemplate("feedback", TemplateType.FEEDBACK)
    )

    @Test
    fun templatesKeepRepositoryOrder() = runTest {
        val viewModel = ChooseTemplateViewModel(FakeTemplateRepository(templates))
        observe(viewModel)

        assertEquals(templates, viewModel.uiState.value.templates)
    }

    @Test
    fun repositoryUpdatesReachTheUiState() = runTest {
        val repository = FakeTemplateRepository(templates)
        val viewModel = ChooseTemplateViewModel(repository)
        observe(viewModel)

        val updatedTemplates = listOf(templates[1])
        repository.templates.value = updatedTemplates
        runCurrent()

        assertEquals(updatedTemplates, viewModel.uiState.value.templates)
    }

    @Test
    fun repositoryCanReturnAnEmptyList() = runTest {
        val repository = FakeTemplateRepository(templates)
        val viewModel = ChooseTemplateViewModel(repository)
        observe(viewModel)

        repository.templates.value = emptyList()
        runCurrent()

        assertTrue(viewModel.uiState.value.templates.isEmpty())
    }

    @Test
    fun recreatedViewModelReadsTheRepositoryAgain() = runTest {
        val repository = FakeTemplateRepository(templates)
        val original = ChooseTemplateViewModel(repository)
        observe(original)

        // no guardamos una tarjeta seleccionada porque el toque abre el editor directamente
        val restored = ChooseTemplateViewModel(repository)
        observe(restored)

        assertEquals(templates, restored.uiState.value.templates)
    }

    private fun TestScope.observe(viewModel: ChooseTemplateViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()
    }
}
