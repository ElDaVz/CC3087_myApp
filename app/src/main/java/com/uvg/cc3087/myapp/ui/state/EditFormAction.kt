package com.uvg.cc3087.myapp.ui.state

import com.uvg.cc3087.myapp.data.model.FormFieldType

// la ui describe el toque y el viewmodel decide cómo cambia el borrador
sealed interface EditFormAction {
    data class ChangeTitle(val title: String) : EditFormAction
    data class AddField(val type: FormFieldType) : EditFormAction
    data class ChangeFieldTitle(val fieldId: String, val title: String) : EditFormAction
    data class SetFieldRequired(val fieldId: String, val required: Boolean) : EditFormAction
    data class MoveField(val fieldId: String, val direction: FieldMoveDirection) : EditFormAction
    data class DeleteField(val fieldId: String) : EditFormAction
}

enum class FieldMoveDirection(val offset: Int) {
    UP(-1),
    DOWN(1)
}
