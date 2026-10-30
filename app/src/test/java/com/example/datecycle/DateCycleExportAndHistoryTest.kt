package com.example.datecycle

import com.example.datecycle.data.calculator.DateCycleCalculator
import com.example.datecycle.data.model.DateFormatOption
import com.example.datecycle.data.model.DateCycleHistory
import com.example.datecycle.data.model.IntervalUnit
import com.example.datecycle.data.model.MonthlyAnchorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateCycleExportAndHistoryTest {

    @Test
    fun testBuildExportTextFormatting() {
        val start = LocalDate.of(2026, 10, 30)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 2,
            intervalUnit = IntervalUnit.WEEKS,
            occurrences = 3
        )

        val formatter = java.time.format.DateTimeFormatter.ofPattern(DateFormatOption.MEDIUM_DATE.pattern)
        val sb = StringBuilder()
        sb.appendLine("DateCycle Repeating Dates")
        sb.appendLine("=========================")
        sb.appendLine("Start Date: ${result.startDate.format(formatter)} (${result.startDate.dayOfWeek})")
        sb.appendLine("Interval: Every ${result.intervalUnit.format(result.intervalValue)}")
        sb.appendLine("Total Dates: ${result.dates.size}")
        sb.appendLine("-------------------------")
        result.dates.forEach { item ->
            val formattedDate = item.date.format(formatter)
            val dayOfWeek = item.date.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() }
            sb.appendLine("#${item.index}. $formattedDate ($dayOfWeek) [+${item.daysFromStart} days]")
        }

        val text = sb.toString()
        assertTrue(text.contains("Start Date: Oct 30, 2026 (FRIDAY)"))
        assertTrue(text.contains("Interval: Every 2 Weeks"))
        assertTrue(text.contains("#1. Oct 30, 2026 (Friday) [+0 days]"))
        assertTrue(text.contains("#2. Nov 13, 2026 (Friday) [+14 days]"))
        assertTrue(text.contains("#3. Nov 27, 2026 (Friday) [+28 days]"))
    }

    @Test
    fun testHistoryModelProperties() {
        val start = LocalDate.of(2026, 5, 1)
        val end = LocalDate.of(2026, 12, 31)
        val history = DateCycleHistory(
            id = "test-123",
            timestamp = 1700000000000L,
            title = "Test Cycle",
            startDateEpochDay = start.toEpochDay(),
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTHS,
            occurrenceCount = 8,
            endDateEpochDay = end.toEpochDay(),
            monthlyAnchorMode = MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY,
            generatedDateCount = 8,
            notes = "Test Note"
        )

        assertEquals("test-123", history.id)
        assertEquals(start, history.startDate)
        assertEquals(end, history.endDate)
        assertEquals(IntervalUnit.MONTHS, history.intervalUnit)
        assertEquals(MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY, history.monthlyAnchorMode)
        assertEquals("Test Note", history.notes)
    }

    @Test
    fun testHistoryModelWithNullEndDateAndNotes() {
        val start = LocalDate.of(2026, 1, 15)
        val history = DateCycleHistory(
            id = "test-456",
            timestamp = 1700000000000L,
            title = "Daily Cycle",
            startDateEpochDay = start.toEpochDay(),
            intervalValue = 3,
            intervalUnit = IntervalUnit.DAYS,
            occurrenceCount = 5,
            endDateEpochDay = null,
            monthlyAnchorMode = MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY,
            generatedDateCount = 5,
            notes = null
        )

        assertNull(history.endDate)
        assertNull(history.notes)
        assertEquals(5, history.occurrenceCount)
    }
}
