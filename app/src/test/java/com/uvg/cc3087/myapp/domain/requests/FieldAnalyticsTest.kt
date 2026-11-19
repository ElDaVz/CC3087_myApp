package com.uvg.cc3087.myapp.domain.requests

import com.uvg.cc3087.myapp.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.*

class FieldAnalyticsTest {
    private val now = Instant.parse("2026-11-19T18:00:00Z")
    private val zone = ZoneId.of("America/Guatemala")
    private fun answer(type: FormFieldType, value: RequestAnswerValue, id: String = type.name,
        options: List<String> = emptyList(), number: String? = null, unit: String = "", time: Boolean = false) =
        RequestDetailAnswer(id, "Pregunta $id", value, RequestFieldSnapshot(type, options, unit, time), number?.toBigDecimal())
    private fun row(id: String, vararg answers: RequestDetailAnswer, date: Instant? = now) =
        RequestDetail("form", id, "Histórico", date, answers.toList())
    private fun analyze(vararg rows: RequestDetail, period: ResponsePeriod = ResponsePeriod.ALL) =
        formAnalytics("form", rows.toList(), period, now, zone)

    @Test fun supportsAllNineComponentsWithoutExposingFreeTextOrEmails() {
        val fields = listOf(
            answer(FormFieldType.TEXT, RequestAnswerValue.Text("hola")),
            answer(FormFieldType.PARAGRAPH, RequestAnswerValue.Text("seis!!")),
            answer(FormFieldType.NUMBER, RequestAnswerValue.Text("1,5"), number = "1.5", unit = "kg"),
            answer(FormFieldType.EMAIL, RequestAnswerValue.Text("privado@example.test")),
            answer(FormFieldType.CHECKBOX, RequestAnswerValue.Checked(false)),
            answer(FormFieldType.MULTIPLE_CHOICE, RequestAnswerValue.Choices(listOf("A")), options = listOf("A", "B")),
            answer(FormFieldType.CHECKBOX_GROUP, RequestAnswerValue.Choices(listOf("A", "B")), options = listOf("A", "B")),
            answer(FormFieldType.DROPDOWN, RequestAnswerValue.Choices(listOf("B")), options = listOf("A", "B")),
            answer(FormFieldType.DATE, RequestAnswerValue.Text("2026-12-01")))
        val result = analyze(row("one", *fields.toTypedArray()))
        assertEquals(9, result.fields.size)
        assertTrue(result.fields.all { it.answered == 1 && it.empty == 0 && it.invalid == 0 })
        assertEquals(4.0, (result.fields[0].statistics as AnswerStatistics.Text).averageLength!!, 0.0)
        assertEquals(BigDecimal("1.5"), (result.fields[2].statistics as AnswerStatistics.Numeric).average)
        assertEquals(1, (result.fields[3].statistics as AnswerStatistics.Email).valid)
        assertEquals(AnswerStatistics.Checkbox(0, 1), result.fields[4].statistics)
        assertEquals(2, (result.fields[6].statistics as AnswerStatistics.Selection).categories.sumOf { it.count })
        assertFalse(result.toString().contains("privado@example.test"))
        assertFalse(result.toString().contains("seis!!"))
    }

    @Test fun numericStatisticsUseCanonicalDecimalsAndDoNotGuessFromDisplayText() {
        val a = answer(FormFieldType.NUMBER, RequestAnswerValue.Text("1,5"), number = "1.5")
        val b = a.copy(value = RequestAnswerValue.Text("3.5"), numericValue = BigDecimal("3.5"))
        val c = a.copy(value = RequestAnswerValue.Text("2000"), numericValue = null)
        val result = analyze(row("a", a), row("b", b), row("c", c)).fields.single()
        val stats = result.statistics as AnswerStatistics.Numeric
        assertEquals(BigDecimal("1.5"), stats.minimum)
        assertEquals(BigDecimal("3.5"), stats.maximum)
        assertEquals(BigDecimal("2.5"), stats.average)
        assertEquals(1, result.invalid)
    }

    @Test fun unknownHistoricalTextStaysUnknownEvenIfItLooksLikeANumber() {
        val old = RequestDetailAnswer("x", "Cantidad", RequestAnswerValue.Text("150"))
        val result = analyze(row("a", old), row("b", old.copy(value = RequestAnswerValue.Text("")))).fields.single()
        assertEquals(AnswerStatistics.Legacy, result.statistics)
        assertEquals(1, result.answered)
        assertEquals(1, result.empty)
    }

    @Test fun changesToTypeLabelOptionsUnitAndTimeAreSeparateHistoricalVariants() {
        val original = answer(FormFieldType.NUMBER, RequestAnswerValue.Text("1"), id = "same", number = "1", unit = "kg")
        val variants = listOf(original, original.copy(label = "Nueva pregunta"),
            original.copy(field = original.field!!.copy(unit = "lb")),
            answer(FormFieldType.TEXT, RequestAnswerValue.Text("1"), id = "same"),
            answer(FormFieldType.MULTIPLE_CHOICE, RequestAnswerValue.Choices(listOf("A")), id = "same", options = listOf("A", "B")),
            answer(FormFieldType.MULTIPLE_CHOICE, RequestAnswerValue.Choices(listOf("A")), id = "same", options = listOf("A", "C")),
            answer(FormFieldType.DATE, RequestAnswerValue.Text("2026-12-01"), id = "same"),
            answer(FormFieldType.DATE, RequestAnswerValue.Text("2026-12-01 10:00"), id = "same", time = true))
        val result = analyze(*variants.mapIndexed { i, a -> row("$i", a) }.toTypedArray())
        assertEquals(8, result.fields.size)
        assertEquals((1..8).toList(), result.fields.map { it.version })
        assertTrue(result.fields.all { it.hasVersions && it.total == 1 })
    }

    @Test fun aQuestionAbsentFromSomeResponsesDoesNotCountThoseAsBlank() {
        val field = answer(FormFieldType.TEXT, RequestAnswerValue.Text("sí"))
        val result = analyze(row("a", field), row("b"))
        assertEquals(2, result.responseCount)
        assertEquals(1, result.fields.single().total)
        assertEquals(0, result.fields.single().empty)
    }

    @Test fun uncheckedIsAnAnswerAndNotABlank() {
        val field = answer(FormFieldType.CHECKBOX, RequestAnswerValue.Checked(false))
        val result = analyze(row("a", field)).fields.single()
        assertEquals(1, result.answered)
        assertEquals(0, result.empty)
        assertEquals(AnswerStatistics.Checkbox(0, 1), result.statistics)
    }

    @Test fun multipleSelectionsCountEachOptionOncePerResponseAndRejectUnknownOptions() {
        val a = answer(FormFieldType.CHECKBOX_GROUP, RequestAnswerValue.Choices(listOf("A", "A", "B")), options = listOf("A", "B"))
        val result = analyze(row("a", a), row("b", a.copy(value = RequestAnswerValue.Choices(listOf("A")))),
            row("c", a.copy(value = RequestAnswerValue.Choices(listOf("X"))))).fields.single()
        val stats = result.statistics as AnswerStatistics.Selection
        assertEquals(listOf(AnswerCategory("A", 2), AnswerCategory("B", 1)), stats.categories)
        assertEquals(2, stats.valid)
        assertEquals(1, result.invalid)
    }

    @Test fun aSingleChoiceCannotSilentlyBecomeMultipleChoice() {
        val field = answer(FormFieldType.DROPDOWN, RequestAnswerValue.Choices(listOf("A", "B")), options = listOf("A", "B"))
        val result = analyze(row("a", field)).fields.single()
        assertEquals(1, result.invalid)
        assertEquals(0, (result.statistics as AnswerStatistics.Selection).valid)
    }

    @Test fun datesUseChosenDatesAndGroupChosenHoursWithoutConvertingTimeZones() {
        val a = answer(FormFieldType.DATE, RequestAnswerValue.Text("2030-01-01 14:30"), time = true)
        val result = analyze(row("a", a), row("b", a.copy(value = RequestAnswerValue.Text("2030-01-02 14:00"))),
            row("c", a.copy(value = RequestAnswerValue.Text("not a date")))).fields.single()
        val stats = result.statistics as AnswerStatistics.Dates
        assertEquals(LocalDateTime.of(2030, 1, 1, 14, 30), stats.first)
        assertEquals(LocalDateTime.of(2030, 1, 2, 14, 0), stats.last)
        assertEquals(listOf(AnswerCategory("14:00", 2)), stats.hours)
        assertEquals(1, result.invalid)
    }

    @Test fun rollingPeriodUsesLocalCalendarAndAllIncludesUndatedHistoricalRows() {
        val field = answer(FormFieldType.TEXT, RequestAnswerValue.Text("x"))
        val source = arrayOf(row("included", field, date = Instant.parse("2026-11-13T06:00:00Z")),
            row("old", field, date = Instant.parse("2026-11-13T05:59:59Z")),
            row("tomorrow", field, date = Instant.parse("2026-11-20T06:00:00Z")), row("undated", field, date = null))
        assertEquals(1, analyze(*source, period = ResponsePeriod.WEEK).responseCount)
        assertEquals(4, analyze(*source).responseCount)
    }

    @Test fun duplicateResponseRowsAreDeduplicatedAndDuplicateQuestionIdsAreExplicitlyExcluded() {
        val field = answer(FormFieldType.TEXT, RequestAnswerValue.Text("x"))
        val a = row("a", field)
        val result = analyze(a, a, row("b", field, field.copy(label = "duplicada")))
        assertEquals(2, result.responseCount)
        assertEquals(1, result.excludedResponses)
        assertEquals(1, result.fields.single().total)
    }

    @Test fun unicodeCharactersAreNotCountedAsUtf16CodeUnits() {
        val result = analyze(row("a", answer(FormFieldType.TEXT, RequestAnswerValue.Text("a😀")))).fields.single()
        assertEquals(2.0, (result.statistics as AnswerStatistics.Text).averageLength!!, 0.0)
    }

    @Test fun emptyAndIncompatibleValuesDoNotCreateZeroAverages() {
        val field = answer(FormFieldType.NUMBER, RequestAnswerValue.Text(""))
        val result = analyze(row("a", field), row("b", field.copy(value = RequestAnswerValue.Checked(true)))).fields.single()
        assertNull((result.statistics as AnswerStatistics.Numeric).average)
        assertEquals(1, result.empty)
        assertEquals(1, result.invalid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun anotherFormsResponsesAreNeverAggregated() {
        analyze(row("a").copy(formId = "other"))
    }
}
