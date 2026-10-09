package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.FormSummary

// la pantalla recibe la lista ya filtrada y la opción que debe marcar
data class FormsUiState(
    val forms: List<FormSummary> = emptyList(),
    val selectedFilter: FormFilter = FormFilter.ALL
)
