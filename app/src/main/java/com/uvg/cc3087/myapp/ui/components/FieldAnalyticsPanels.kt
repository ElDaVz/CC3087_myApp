package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.domain.requests.*
import com.uvg.cc3087.myapp.ui.state.RequestListLoadStatus
import com.uvg.cc3087.myapp.ui.viewmodel.FieldAnalyticsUiState
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun LazyListScope.fieldAnalyticsPanels(state: FieldAnalyticsUiState, onRetry: () -> Unit) {
    item("field-analytics-heading") {
        Text(stringResource(R.string.field_analytics_title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("field-analytics-heading").semantics { heading() })
    }
    if (state.formId == null) {
        item("field-analytics-select") { Text(stringResource(R.string.field_analytics_select_form)) }
    } else if (state.status != RequestListLoadStatus.CONTENT) {
        item("field-analytics-load") { ResponseLoadPanel(state.status, onRetry) }
    } else {
        item("field-analytics-summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.field_analytics_responses, state.analytics.responseCount),
                    modifier = Modifier.testTag("field-analytics-total"), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.field_analytics_scope), style = MaterialTheme.typography.bodySmall)
                if (state.analytics.excludedResponses > 0) {
                    Text(stringResource(R.string.field_analytics_excluded, state.analytics.excludedResponses),
                        color = MaterialTheme.colorScheme.error)
                }
                if (state.analytics.fields.isEmpty()) Text(stringResource(R.string.field_analytics_empty))
            }
        }
        itemsIndexed(state.analytics.fields, key = { index, _ -> "field-analytics-$index" }) { index, field ->
            FieldAnalyticsCard(field, index)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FieldAnalyticsCard(field: FieldAnalytics, index: Int) {
    OutlinedCard(Modifier.fillMaxWidth().testTag("field-analytics-card-$index")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(field.snapshot?.type?.let { stringResource(it.labelRes) }
                ?: stringResource(R.string.field_analytics_legacy_type),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(field.label.ifBlank { stringResource(R.string.request_detail_unnamed_field) },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("field-analytics-question-$index").semantics { heading() })
            if (field.hasVersions) Text(stringResource(R.string.field_analytics_version, field.version),
                style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.field_analytics_answered, field.answered, field.total))
                Text(stringResource(R.string.field_analytics_blank, field.empty))
            }
            if (field.invalid > 0) Text(stringResource(R.string.field_analytics_invalid, field.invalid),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            HorizontalDivider()
            when (val stats = field.statistics) {
                is AnswerStatistics.Text -> AnalyticsMetric(R.string.field_analytics_length, stats.averageLength?.let { formatted(it) })
                is AnswerStatistics.Numeric -> {
                    field.snapshot?.unit?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.field_analytics_unit, it)) }
                    Text(stringResource(R.string.field_analytics_number_precision), style = MaterialTheme.typography.bodySmall)
                    AnalyticsMetric(R.string.field_analytics_minimum, stats.minimum?.let { formatted(it) })
                    AnalyticsMetric(R.string.field_analytics_maximum, stats.maximum?.let { formatted(it) })
                    AnalyticsMetric(R.string.field_analytics_average, stats.average?.let { formatted(it) })
                }
                is AnswerStatistics.Email -> {
                    Text(stringResource(R.string.field_analytics_valid_email, stats.valid))
                    Text(stringResource(R.string.field_analytics_email_private), style = MaterialTheme.typography.bodySmall)
                }
                is AnswerStatistics.Checkbox -> {
                    AnalyticsCategories(listOf(AnswerCategory(stringResource(R.string.request_detail_yes), stats.yes),
                        AnswerCategory(stringResource(R.string.request_detail_no), stats.no)), stats.yes + stats.no)
                }
                is AnswerStatistics.Selection -> {
                    Text(stringResource(if (stats.multiple) R.string.field_analytics_multiple_hint else
                        R.string.field_analytics_selection_hint), style = MaterialTheme.typography.bodySmall)
                    AnalyticsCategories(stats.categories, stats.valid)
                }
                is AnswerStatistics.Dates -> {
                    val locale = LocalResources.current.configuration.locales[0]
                    val formatter = if (field.snapshot?.includeTime == true) DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    AnalyticsMetric(R.string.field_analytics_first_date, stats.first?.let { formatter.withLocale(locale).format(it) })
                    AnalyticsMetric(R.string.field_analytics_last_date, stats.last?.let { formatter.withLocale(locale).format(it) })
                    Text(stringResource(R.string.field_analytics_by_date), style = MaterialTheme.typography.titleSmall)
                    AnalyticsCategories(stats.days, stats.valid)
                    if (field.snapshot?.includeTime == true) {
                        Text(stringResource(R.string.field_analytics_by_hour), style = MaterialTheme.typography.titleSmall)
                        AnalyticsCategories(stats.hours, stats.valid)
                    }
                }
                AnswerStatistics.Legacy -> Text(stringResource(R.string.field_analytics_legacy_hint),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AnalyticsMetric(label: Int, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value ?: stringResource(R.string.field_analytics_no_values), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AnalyticsCategories(categories: List<AnswerCategory>, denominator: Int) {
    if (denominator == 0 || categories.isEmpty()) {
        Text(stringResource(R.string.field_analytics_no_values))
        return
    }
    categories.take(8).forEach { category ->
        val fraction = category.count.toFloat() / denominator
        val percent = NumberFormat.getPercentInstance(LocalResources.current.configuration.locales[0]).format(fraction.toDouble())
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(category.label, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.field_analytics_category_count, category.count, denominator, percent),
                style = MaterialTheme.typography.labelMedium)
            // la cifra siempre acompaña a la barra para no depender solo del color
            Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small))
            }
        }
    }
    if (categories.size > 8) Text(stringResource(R.string.field_analytics_more_categories, categories.size - 8),
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun formatted(value: Number): String = NumberFormat.getNumberInstance(LocalResources.current.configuration.locales[0])
    .apply { maximumFractionDigits = 3 }.format(value)
