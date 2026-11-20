package com.uvg.cc3087.myapp.ui.state

/** Local preference. IDs break ties so every order is stable and reproducible. */
enum class FormSortOrder {
    RECENT_FIRST,
    OLDEST_FIRST,
    MOST_RESPONSES,
    FEWEST_RESPONSES
}
