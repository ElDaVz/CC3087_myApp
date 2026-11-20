package com.uvg.cc3087.myapp.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.uvg.cc3087.myapp.data.model.RequestStatus
import kotlinx.serialization.Serializable

// las rutas llevan identificadores, nunca títulos, respuestas ni credenciales
@Serializable
internal sealed interface AppRoute : NavKey {
    @Serializable data object Forms : AppRoute
    @Serializable data object ChooseTemplate : AppRoute
    @Serializable data class Editor(val draftId: String) : AppRoute {
        init { require(draftId.isNotBlank()) }
    }
    @Serializable data class Preview(val draftId: String) : AppRoute {
        init { require(draftId.isNotBlank()) }
    }
    @Serializable data class SaveShare(val draftId: String) : AppRoute {
        init { require(draftId.isNotBlank()) }
    }
    @Serializable data object Dashboard : AppRoute
    @Serializable data class Responses(val filter: InboxFilter = InboxFilter.ALL) : AppRoute
    @Serializable data class RequestDetail(val formId: String, val responseId: String) : AppRoute {
        init { require(formId.isNotBlank() && responseId.isNotBlank()) }
    }
    @Serializable data object Settings : AppRoute
    @Serializable data object Statistics : AppRoute
    @Serializable data object Account : AppRoute
}

@Serializable
internal enum class InboxFilter {
    ALL, PENDING, IN_PROGRESS, COMPLETED, UNKNOWN;

    fun matches(status: RequestStatus?): Boolean = when (this) {
        ALL -> true
        UNKNOWN -> status == null
        else -> status?.name == name
    }

    companion object {
        fun fromKey(key: String): InboxFilter = entries.firstOrNull { it.name == key } ?: ALL
    }
}
