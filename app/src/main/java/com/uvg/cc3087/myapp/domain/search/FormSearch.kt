package com.uvg.cc3087.myapp.domain.search

import com.uvg.cc3087.myapp.data.model.FormSummary
import java.text.Normalizer
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

/** Local title matching only. Stable ties retain the repository's order. */
internal object FormSearch {
    private val marks = Regex("\\p{M}+")
    private val spaces = Regex("\\s+")

    private fun normalize(value: String): String = marks.replace(
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD), ""
    ).trim().replace(spaces, " ")

    suspend fun find(forms: List<FormSummary>, query: String): List<FormSummary> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return forms
        val words = normalized.split(' ')
        return forms.mapNotNull { form ->
            coroutineContext.ensureActive()
            val title = normalize(form.title)
            val titleWords = title.split(' ')
            val rank = when {
                title == normalized -> 0
                title.startsWith(normalized) -> 1
                words.all { word -> titleWords.any { word in it } } -> 2
                words.all { word -> titleWords.any { candidate ->
                    word in candidate || withinEdits(word, candidate)
                } } -> 3
                else -> return@mapNotNull null
            }
            form to rank
        }.sortedBy { it.second }.map { it.first }
    }

    // Bounded optimal string alignment: insertion, deletion, substitution and
    // adjacent transposition. Partial words are handled above, without fuzziness.
    private suspend fun withinEdits(query: String, word: String): Boolean {
        val limit = when (query.length) {
            in 0..3 -> return false
            in 4..7 -> 1
            else -> 2
        }
        if (kotlin.math.abs(query.length - word.length) > limit) return false
        var previousPrevious = IntArray(word.length + 1)
        var previous = IntArray(word.length + 1) { it }
        for (i in 1..query.length) {
            coroutineContext.ensureActive()
            val current = IntArray(word.length + 1) { limit + 1 }
            current[0] = i
            for (j in maxOf(1, i - limit)..minOf(word.length, i + limit)) {
                current[j] = minOf(
                    previous[j] + 1, current[j - 1] + 1,
                    previous[j - 1] + if (query[i - 1] == word[j - 1]) 0 else 1
                )
                if (i > 1 && j > 1 && query[i - 1] == word[j - 2] && query[i - 2] == word[j - 1]) {
                    current[j] = minOf(current[j], previousPrevious[j - 2] + 1)
                }
            }
            if (current.min() > limit) return false
            previousPrevious = previous
            previous = current
        }
        return previous[word.length] <= limit
    }
}
