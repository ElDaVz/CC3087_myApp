package com.uvg.cc3087.myapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.cc3087.myapp.data.model.ThemeMode
import com.uvg.cc3087.myapp.data.repository.AppearanceRepository
import com.uvg.cc3087.myapp.ui.viewmodel.AppearanceUiState
import com.uvg.cc3087.myapp.ui.viewmodel.AppearanceViewModel

internal data class AppearanceActions(
    val state: AppearanceUiState = AppearanceUiState(),
    val select: (ThemeMode) -> Unit = {},
    val retry: () -> Unit = {}
)

internal val LocalAppearance = staticCompositionLocalOf { AppearanceActions() }

/** Lives above the owner-scoped navigation so signing out cannot reset appearance. */
@Composable
internal fun AppearanceRoot(
    repository: AppearanceRepository,
    onDarkThemeChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val model: AppearanceViewModel = viewModel(factory = AppearanceViewModel.factory(repository))
    val state by model.uiState.collectAsStateWithLifecycle()
    val dark = state.mode.isDark(isSystemInDarkTheme())
    SideEffect { onDarkThemeChanged(dark) }
    MyappTheme(darkTheme = dark, dynamicColor = false) {
        CompositionLocalProvider(LocalAppearance provides AppearanceActions(state, model::selectTheme, model::retry)) {
            Surface(Modifier.fillMaxSize()) {
                if (state.loaded) content()
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}
