package com.example.datecycle

import com.example.datecycle.data.calculator.DateCycleCalculator
import com.example.datecycle.data.model.IntervalUnit
import com.example.datecycle.data.model.MonthlyAnchorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateCycleCalculatorTest {

    @Test
    fun testDailyInterval() {
        val start = LocalDate.of(2026, 1, 1)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 3,
            intervalUnit = IntervalUnit.DAYS,
            occurrences = 4
        )
        assertEquals(4, result.dates.size)
        assertEquals(LocalDate.of(2026, 1, 1), result.dates[0].date)
        assertEquals(LocalDate.of(2026, 1, 4), result.dates[1].date)
        assertEquals(LocalDate.of(2026, 1, 7), result.dates[2].date)
        assertEquals(LocalDate.of(2026, 1, 10), result.dates[3].date)
    }

    @Test
    fun testWeeklyInterval() {
        val start = LocalDate.of(2026, 10, 5) // Monday
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 2,
            intervalUnit = IntervalUnit.WEEKS,
            occurrences = 3
        )
        assertEquals(3, result.dates.size)
        assertEquals(LocalDate.of(2026, 10, 5), result.dates[0].date)
        assertEquals(LocalDate.of(2026, 10, 19), result.dates[1].date)
        assertEquals(LocalDate.of(2026, 11, 2), result.dates[2].date)
    }

    @Test
    fun testMonthlyIntervalPreserveAnchor() {
        // Start on January 31, 2024 (leap year)
        val start = LocalDate.of(2024, 1, 31)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTHS,
            occurrences = 5,
            monthlyAnchorMode = MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY
        )
        assertEquals(5, result.dates.size)
        assertEquals(LocalDate.of(2024, 1, 31), result.dates[0].date)
        assertEquals(LocalDate.of(2024, 2, 29), result.dates[1].date) // Leap year Feb ends on 29
        assertEquals(LocalDate.of(2024, 3, 31), result.dates[2].date) // Recovers 31st!
        assertEquals(LocalDate.of(2024, 4, 30), result.dates[3].date) // April has 30 days
        assertEquals(LocalDate.of(2024, 5, 31), result.dates[4].date) // Recovers 31st!
    }

    @Test
    fun testMonthlyIntervalClampAndCascade() {
        // Start on January 31, 2024
        val start = LocalDate.of(2024, 1, 31)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTHS,
            occurrences = 3,
            monthlyAnchorMode = MonthlyAnchorMode.CLAMP_AND_CASCADE
        )
        assertEquals(3, result.dates.size)
        assertEquals(LocalDate.of(2024, 1, 31), result.dates[0].date)
        assertEquals(LocalDate.of(2024, 2, 29), result.dates[1].date)
        // With clamp and cascade, next month keeps the clamped day 29:
        assertEquals(LocalDate.of(2024, 3, 29), result.dates[2].date)
    }

    @Test
    fun testMonthEndDatesJan29Jan30Jan31NonLeapYear() {
        // Jan 29 in non-leap year (2025)
        val jan29 = LocalDate.of(2025, 1, 29)
        val res29 = DateCycleCalculator.calculate(
            startDate = jan29,
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTHS,
            occurrences = 3
        )
        assertEquals(LocalDate.of(2025, 1, 29), res29.dates[0].date)
        assertEquals(LocalDate.of(2025, 2, 28), res29.dates[1].date)
        assertEquals(LocalDate.of(2025, 3, 29), res29.dates[2].date)

        // Jan 30 in non-leap year (2025)
        val jan30 = LocalDate.of(2025, 1, 30)
        val res30 = DateCycleCalculator.calculate(
            startDate = jan30,
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTHS,
            occurrences = 3
        )
        assertEquals(LocalDate.of(2025, 1, 30), res30.dates[0].date)
        assertEquals(LocalDate.of(2025, 2, 28), res30.dates[1].date)
        assertEquals(LocalDate.of(2025, 3, 30), res30.dates[2].date)

        // Jan 31 in non-leap year (2025)
        val jan31 = LocalDate.of(2025, 1, 31)
        val res31 = DateCycleCalculator.calculate(
            startDate = jan31,
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTHS,
            occurrences = 3
        )
        assertEquals(LocalDate.of(2025, 1, 31), res31.dates[0].date)
        assertEquals(LocalDate.of(2025, 2, 28), res31.dates[1].date)
        assertEquals(LocalDate.of(2025, 3, 31), res31.dates[2].date)
    }

    @Test
    fun testYearlyIntervalLeapYear() {
        // Feb 29, 2024 (leap year)
        val start = LocalDate.of(2024, 2, 29)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 1,
            intervalUnit = IntervalUnit.YEARS,
            occurrences = 5
        )
        assertEquals(5, result.dates.size)
        assertEquals(LocalDate.of(2024, 2, 29), result.dates[0].date)
        assertEquals(LocalDate.of(2025, 2, 28), result.dates[1].date)
        assertEquals(LocalDate.of(2026, 2, 28), result.dates[2].date)
        assertEquals(LocalDate.of(2027, 2, 28), result.dates[3].date)
        assertEquals(LocalDate.of(2028, 2, 29), result.dates[4].date) // 2028 is a leap year!
    }

    @Test
    fun testEndDateRestriction() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 15)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 5,
            intervalUnit = IntervalUnit.DAYS,
            occurrences = 10,
            endDate = end
        )
        // Days: Jan 1, Jan 6, Jan 11 (next is Jan 16, which is > Jan 15)
        assertEquals(3, result.dates.size)
        assertEquals(LocalDate.of(2026, 1, 1), result.dates[0].date)
        assertEquals(LocalDate.of(2026, 1, 6), result.dates[1].date)
        assertEquals(LocalDate.of(2026, 1, 11), result.dates[2].date)
        assertTrue(result.stoppedByEndDate)
    }

    @Test
    fun testSingleOccurrence() {
        val start = LocalDate.of(2026, 6, 15)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 2,
            intervalUnit = IntervalUnit.MONTHS,
            occurrences = 1
        )
        assertEquals(1, result.dates.size)
        assertEquals(start, result.dates[0].date)
        assertFalse(result.stoppedByEndDate)
    }

    @Test
    fun testMaxOccurrencesLimit() {
        val start = LocalDate.of(2026, 1, 1)
        val result = DateCycleCalculator.calculate(
            startDate = start,
            intervalValue = 1,
            intervalUnit = IntervalUnit.DAYS,
            occurrences = 600 // Requesting over max 500
        )
        assertEquals(500, result.dates.size)
        assertEquals(500, result.requestedOccurrences)
    }

    @Test
    fun testValidation() {
        val validDate = LocalDate.of(2026, 5, 1)

        // Null start date
        assertTrue(DateCycleCalculator.validate(null, 1, IntervalUnit.DAYS, 5, null) is DateCycleCalculator.ValidationResult.Error)

        // Interval <= 0
        assertTrue(DateCycleCalculator.validate(validDate, 0, IntervalUnit.DAYS, 5, null) is DateCycleCalculator.ValidationResult.Error)

        // Occurrences <= 0
        assertTrue(DateCycleCalculator.validate(validDate, 1, IntervalUnit.DAYS, 0, null) is DateCycleCalculator.ValidationResult.Error)

        // Occurrences > 500
        assertTrue(DateCycleCalculator.validate(validDate, 1, IntervalUnit.DAYS, 501, null) is DateCycleCalculator.ValidationResult.Error)

        // End date before start date
        val earlierEnd = LocalDate.of(2026, 4, 30)
        assertTrue(DateCycleCalculator.validate(validDate, 1, IntervalUnit.DAYS, 5, earlierEnd) is DateCycleCalculator.ValidationResult.Error)

        // Valid
        assertTrue(DateCycleCalculator.validate(validDate, 2, IntervalUnit.WEEKS, 10, null) is DateCycleCalculator.ValidationResult.Valid)
    }
}
