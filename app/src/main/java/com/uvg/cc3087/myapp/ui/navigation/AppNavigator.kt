package com.uvg.cc3087.myapp.ui.navigation

import androidx.navigation3.runtime.NavKey

// navdisplay observa esta pila y mantiene el estado de las pantallas que siguen dentro
internal class AppNavigator(private val backStack: MutableList<NavKey>) {
    init { require(backStack.firstOrNull() == AppRoute.Forms) }

    fun isCurrent(route: AppRoute): Boolean = backStack.lastOrNull() == route

    fun navigate(route: AppRoute) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun backFrom(route: AppRoute) {
        if (isCurrent(route)) back()
    }

    fun showForms() {
        backStack.subList(1, backStack.size).clear()
    }

    fun showSettings() {
        val existing = backStack.indexOfLast { it == AppRoute.Settings }
        if (existing >= 0) backStack.subList(existing + 1, backStack.size).clear()
        else navigate(AppRoute.Settings)
    }

    fun showDashboard() {
        if (backStack.lastOrNull() == AppRoute.Dashboard) return
        showForms()
        navigate(AppRoute.Dashboard)
    }

    fun showSaveShare(draftId: String) {
        // continuar desde la vista previa vuelve al editor, no a respuestas de prueba
        if (backStack.lastOrNull() is AppRoute.Preview) back()
        navigate(AppRoute.SaveShare(draftId))
    }
}
