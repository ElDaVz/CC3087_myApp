package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.FormDraft

data class SaveShareUiState(
    val draft: FormDraft? = null,
    val isLoading: Boolean = false,
    val loadError: DraftLoadError? = null,
    // solo una publicación real podrá proporcionar este enlace
    val publicLink: String? = null,
    val publication: com.uvg.cc3087.myapp.data.repository.FormPublication? = null,
    val publishing: Boolean = false, val publishError: String? = null
) {
    val canShare: Boolean get() = !publicLink.isNullOrBlank()
}
