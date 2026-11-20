package com.uvg.cc3087.myapp.ui.state

/** Keeps search open when Back is first used to dismiss the software keyboard. */
internal enum class FormsSearchBackAction { IGNORE, HIDE_IME, CLOSE_SEARCH }

internal fun formsSearchBackAction(searchExpanded: Boolean, imeVisible: Boolean): FormsSearchBackAction = when {
    !searchExpanded -> FormsSearchBackAction.IGNORE
    imeVisible -> FormsSearchBackAction.HIDE_IME
    else -> FormsSearchBackAction.CLOSE_SEARCH
}
