package com.uvg.cc3087.myapp.domain.search

import com.uvg.cc3087.myapp.data.model.FormSummary
import com.uvg.cc3087.myapp.ui.state.FormSortOrder

/** Pure, stable ordering used both for the whole list and inside each relevance group. */
internal object FormSort {
    fun apply(forms: List<FormSummary>, order: FormSortOrder): List<FormSummary> = when (order) {
        FormSortOrder.RECENT_FIRST -> forms.sortedWith(
            compareByDescending<FormSummary> { it.updatedAt }.thenBy { it.id }
        )
        FormSortOrder.OLDEST_FIRST -> forms.sortedWith(
            compareBy<FormSummary> { it.updatedAt }.thenBy { it.id }
        )
        FormSortOrder.MOST_RESPONSES -> forms.sortedWith(
            compareByDescending<FormSummary> { it.responseCount }
                .thenByDescending { it.updatedAt }.thenBy { it.id }
        )
        FormSortOrder.FEWEST_RESPONSES -> forms.sortedWith(
            compareBy<FormSummary> { it.responseCount }
                .thenByDescending { it.updatedAt }.thenBy { it.id }
        )
    }
}
