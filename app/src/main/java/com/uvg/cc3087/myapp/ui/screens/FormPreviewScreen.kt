package com.uvg.cc3087.myapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import com.uvg.cc3087.myapp.ui.components.FormAnswerInput
import com.uvg.cc3087.myapp.ui.components.DraftSaveNotice
import com.uvg.cc3087.myapp.ui.state.DraftSaveStatus
import com.uvg.cc3087.myapp.ui.components.currentFormLocale
import com.uvg.cc3087.myapp.ui.components.fieldErrorText
import com.uvg.cc3087.myapp.R
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPreviewScreen(
    title: String,
    fields: List<FormFieldDraft>,
    onClose: () -> Unit,
    validationResult: FormValidationResult,
    configurationErrors: Map<String, Set<FieldError>>,
    validateAnswers: (Map<String, FormAnswer>, Boolean, Locale) -> Map<String, FieldError>,
    onSaveForm: (() -> Unit)? = null,
    saveStatus: DraftSaveStatus = DraftSaveStatus.IDLE,
    onRetrySave: () -> Unit = {},
    canSaveForm: Boolean = false
) {
    var answers by rememberSaveable { mutableStateOf(hashMapOf<String, FormAnswer>()) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val locale = currentFormLocale()
    val focusManager = LocalFocusManager.current
    val answerErrors = validateAnswers(answers, attempted, locale)
    val configurationValid = validationResult == FormValidationResult.READY

    fun testSubmission() {
        attempted = true
        focusManager.clearFocus()
        val errors = validateAnswers(answers, true, locale)
        val firstError = fields.indexOfFirst { it.id in errors }
        submitted = firstError == -1
        if (firstError >= 0) scope.launch { listState.animateScrollToItem(firstError + 1) }
    }

    BackHandler(onBack = onClose)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.preview)) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back_to_editor))
                }
            },
            actions = {
                TextButton(onClick = { answers = hashMapOf(); attempted = false; submitted = false }) {
                    Text(stringResource(R.string.reset))
                }
            }
        )
    }) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title.ifBlank { stringResource(R.string.untitled_form) }, style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.preview_notice))
                    Text(stringResource(R.string.required_legend), style = MaterialTheme.typography.bodySmall)
                    if (!configurationValid) {
                        Text(
                            stringResource(R.string.preview_configuration_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            itemsIndexed(fields, key = { _, field -> field.id }) { index, field ->
                val answer = answers[field.id] ?: FormAnswer()
                val error = answerErrors[field.id]
                val question = stringResource(R.string.numbered_question, index + 1,
                    field.title.ifBlank { stringResource(R.string.untitled_question) })
                val questionLabel = if (field.required) stringResource(R.string.required_question, question) else question
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (field.type != FormFieldType.CHECKBOX) {
                            Text(questionLabel, style = MaterialTheme.typography.titleMedium)
                            if (field.helpText.isNotBlank()) Text(field.helpText, style = MaterialTheme.typography.bodySmall)
                        }
                        configurationErrors[field.id].orEmpty().forEach { issue ->
                            Text(fieldErrorText(issue, field), color = MaterialTheme.colorScheme.error)
                        }
                        if (field.type.hasOptions) {
                            Text(
                                stringResource(if (field.isMultipleSelection) R.string.select_several else R.string.select_one),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FormAnswerInput(field, answer, onChange = {
                            answers = HashMap(answers).apply { put(field.id, it) }
                            submitted = false
                        }, isError = error != null, questionLabel = questionLabel)
                        if (field.type == FormFieldType.CHECKBOX && field.helpText.isNotBlank()) {
                            Text(field.helpText, style = MaterialTheme.typography.bodySmall)
                        }
                        if (error != null) {
                            Text(fieldErrorText(error, field), color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onSaveForm != null) {
                        DraftSaveNotice(saveStatus, onRetrySave)
                        Button(
                            enabled = canSaveForm,
                            onClick = onSaveForm,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.save_form)) }
                        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.back_to_editing))
                        }
                    }
                    if (submitted && configurationValid && answerErrors.isEmpty()) {
                        Text(stringResource(R.string.preview_success),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (onSaveForm == null) {
                        Button(enabled = configurationValid, onClick = ::testSubmission, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.test_submit))
                        }
                    } else {
                        TextButton(enabled = configurationValid, onClick = ::testSubmission, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.test_submit))
                        }
                    }
                }
            }
        }
    }
}
