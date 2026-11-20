package com.uvg.cc3087.myapp.ui.viewmodel

import com.uvg.cc3087.myapp.data.model.ThemeMode
import com.uvg.cc3087.myapp.data.repository.AppearanceRepository
import com.uvg.cc3087.myapp.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun modeResolutionAndUnknownValuesUseSystemByDefault() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("obsolete"))
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStored(it.name)) }
        assertTrue(ThemeMode.SYSTEM.isDark(true))
        assertFalse(ThemeMode.SYSTEM.isDark(false))
        assertFalse(ThemeMode.LIGHT.isDark(true))
        assertTrue(ThemeMode.DARK.isDark(false))
    }

    @Test fun startupWaitsForTheStoredPreference() = runTest {
        val repository = FakeAppearanceRepository()
        val model = AppearanceViewModel(repository)
        assertFalse(model.uiState.value.loaded)
        runCurrent()
        assertTrue(model.uiState.value.loaded)
        assertEquals(ThemeMode.SYSTEM, model.uiState.value.mode)
    }

    @Test fun failedWriteKeepsTheConfirmedThemeAndRetrySavesTheRequestedMode() = runTest {
        val repository = FakeAppearanceRepository().apply { failWrite = true }
        val model = AppearanceViewModel(repository)
        runCurrent()
        model.selectTheme(ThemeMode.DARK)
        runCurrent()
        assertEquals(ThemeMode.SYSTEM, model.uiState.value.mode)
        assertEquals(ThemeMode.DARK, model.uiState.value.failedMode)
        assertFalse(model.uiState.value.saving)
        repository.failWrite = false
        model.retry()
        runCurrent()
        assertEquals(ThemeMode.DARK, model.uiState.value.mode)
        assertFalse(model.uiState.value.failed)
    }

    @Test fun readFailureAllowsTheAppToOpenAndCanBeRetried() = runTest {
        val repository = FakeAppearanceRepository().apply {
            failRead = true
            value.value = ThemeMode.LIGHT
        }
        val model = AppearanceViewModel(repository)
        runCurrent()
        assertTrue(model.uiState.value.loaded)
        assertTrue(model.uiState.value.readFailed)
        assertEquals(ThemeMode.SYSTEM, model.uiState.value.mode)
        repository.failRead = false
        model.retry()
        runCurrent()
        assertEquals(ThemeMode.LIGHT, model.uiState.value.mode)
        assertFalse(model.uiState.value.failed)
    }

    @Test fun duplicateWritesAreIgnoredAndTheThemeOnlyChangesAfterConfirmation() = runTest {
        val repository = FakeAppearanceRepository().apply { gate = CompletableDeferred() }
        val model = AppearanceViewModel(repository)
        runCurrent()
        model.selectTheme(ThemeMode.DARK)
        model.selectTheme(ThemeMode.LIGHT)
        runCurrent()
        assertTrue(model.uiState.value.saving)
        assertEquals(1, repository.writes)
        assertEquals(ThemeMode.SYSTEM, model.uiState.value.mode)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertEquals(ThemeMode.DARK, model.uiState.value.mode)
    }

    private class FakeAppearanceRepository : AppearanceRepository {
        val value = MutableStateFlow(ThemeMode.SYSTEM)
        var failRead = false
        var failWrite = false
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null
        override val themeMode = flow {
            if (failRead) throw IOException("read")
            emitAll(value)
        }
        override suspend fun setThemeMode(mode: ThemeMode) {
            writes++
            gate?.await()
            if (failWrite) throw IOException("write")
            value.value = mode
        }
    }
}
