package com.uvg.cc3087.myapp.ui.state

import org.junit.Assert.assertEquals
import org.junit.Test

class FormsSearchBackActionTest {
    @Test fun backDismissesKeyboardBeforeClosingExpandedSearch() {
        assertEquals(FormsSearchBackAction.HIDE_IME, formsSearchBackAction(searchExpanded = true, imeVisible = true))
        assertEquals(FormsSearchBackAction.CLOSE_SEARCH, formsSearchBackAction(searchExpanded = true, imeVisible = false))
        assertEquals(FormsSearchBackAction.IGNORE, formsSearchBackAction(searchExpanded = false, imeVisible = false))
    }
}
