package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.ThemeMode
import com.uvg.cc3087.myapp.data.repository.AppearanceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppearanceUiState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val readFailed: Boolean = false,
    val failedMode: ThemeMode? = null
) {
    val failed: Boolean get() = readFailed || failedMode != null
}

class AppearanceViewModel(private val repository: AppearanceRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AppearanceUiState())
    val uiState = mutableState.asStateFlow()
    private var observation: Job? = null

    init { observe() }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                repository.themeMode.collect { mode ->
                    mutableState.update { it.copy(mode = mode, loaded = true, readFailed = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loaded = true, readFailed = true) }
            }
        }
    }

    fun selectTheme(mode: ThemeMode) {
        if (!uiState.value.loaded || uiState.value.saving) return
        mutableState.update { it.copy(saving = true, failedMode = null) }
        viewModelScope.launch {
            try {
                repository.setThemeMode(mode)
                mutableState.update { it.copy(mode = mode, saving = false) }
                if (uiState.value.readFailed) observe()
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(saving = false) }
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(saving = false, failedMode = mode) }
            }
        }
    }

    fun retry() {
        if (uiState.value.saving) return
        uiState.value.failedMode?.let(::selectTheme) ?: observe()
    }

    companion object {
        fun factory(repository: AppearanceRepository) = viewModelFactory {
            initializer { AppearanceViewModel(repository) }
        }
    }
}
