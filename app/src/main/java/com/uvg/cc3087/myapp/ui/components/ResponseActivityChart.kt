package com.uvg.cc3087.myapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.domain.requests.ResponseBucket
import com.uvg.cc3087.myapp.domain.requests.ResponseFilter
import com.uvg.cc3087.myapp.domain.requests.ResponsePeriod
import java.time.format.DateTimeFormatter

@Composable
internal fun ResponseActivityChart(buckets: List<ResponseBucket>, filter: ResponseFilter) {
    val period = filter.period
    val locale = LocalResources.current.configuration.locales[0]
    val formatter = remember(locale, period) {
        DateTimeFormatter.ofPattern(if (period == ResponsePeriod.ALL) "MMM yyyy" else "d MMM", locale)
    }
    val maximum = buckets.maxOfOrNull { it.count } ?: 0
    val chartState = rememberLazyListState()
    val columnWidth = 64.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    LaunchedEffect(filter, buckets.firstOrNull()?.date, buckets.lastOrNull()?.date) {
        if (buckets.isNotEmpty()) {
            // en el historial enfocamos el último mes con actividad, aunque sea de años anteriores
            val target = if (period == ResponsePeriod.ALL) buckets.indexOfLast { it.count > 0 }.coerceAtLeast(0)
                else buckets.lastIndex
            chartState.scrollToItem(target)
        }
    }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.dashboard_evolution), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            if (maximum == 0) {
                Text(stringResource(R.string.dashboard_no_dated_activity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(if (period == ResponsePeriod.ALL) R.string.dashboard_monthly else R.string.dashboard_daily),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    state = chartState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("response-chart")
                ) {
                    items(buckets, key = { it.date.toString() }) { bucket ->
                        val label = formatter.format(bucket.date)
                        val description = pluralStringResource(R.plurals.dashboard_chart_value, bucket.count, label, bucket.count)
                        Column(
                            Modifier.width(columnWidth).clearAndSetSemantics { contentDescription = description },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(bucket.count.toString(), style = MaterialTheme.typography.labelMedium)
                            Box(Modifier.height(104.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                // un valor cero no dibuja una barra que parezca una respuesta
                                if (bucket.count > 0) {
                                    Box(Modifier.width(24.dp).height((104f * bucket.count / maximum).dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
