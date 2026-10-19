package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.FormTemplate

data class ChooseTemplateUiState(
    val templates: List<FormTemplate> = emptyList()
)
