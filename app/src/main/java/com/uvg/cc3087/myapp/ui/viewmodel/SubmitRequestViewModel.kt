package com.uvg.cc3087.myapp.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.domain.validation.FormDraftValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class SubmitRequestUiState(
    val form: FormDraft? = null,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false
)

class SubmitRequestViewModel(
    private val forms: FormRepository,
    private val requests: RequestRepository,
    private val formId: String,
    private val responseId: String
) : ViewModel() {
    private val mutableState = MutableStateFlow(SubmitRequestUiState())
    val uiState = mutableState.asStateFlow()
    init { load() }

    fun load() {
        mutableState.value = SubmitRequestUiState()
        viewModelScope.launch {
            try {
                // Restore completion after process recreation without submitting twice.
                val existing = requests.getDetail(formId, responseId)
                val form = forms.getDraft(formId)
                mutableState.value = SubmitRequestUiState(form = form, loading = false,
                    loadFailed = form == null && existing == null, saved = existing != null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = SubmitRequestUiState(loading = false, loadFailed = true) }
        }
    }

    fun validationResult() = mutableState.value.form.let { form ->
        FormDraftValidator.validate(form?.title.orEmpty(), form?.fields.orEmpty())
    }

    fun configurationErrors() = FormDraftValidator.configurationErrors(
        mutableState.value.form?.fields.orEmpty()
    )

    fun validateAnswers(answers: Map<String, FormAnswer>, attempted: Boolean, locale: Locale) =
        FormDraftValidator.validateAnswers(
            mutableState.value.form?.fields.orEmpty(), answers, attempted, LocalDate.now(), locale
        )

    fun submit(answers: Map<String, FormAnswer>, locale: Locale) {
        val state = mutableState.value
        val form = state.form ?: return
        if (state.saving || state.saved) return
        mutableState.value = state.copy(saving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                requests.submit(form, responseId, answers.toMap(), locale)
                mutableState.value = state.copy(saved = true, saving = false, saveFailed = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.value = state.copy(saveFailed = true) }
        }
    }

    companion object {
        fun factory(forms: FormRepository, requests: RequestRepository, formId: String, responseId: String) = viewModelFactory {
            initializer { SubmitRequestViewModel(forms, requests, formId, responseId) }
        }
    }
}
