package com.example.datecycle.data.model

import java.time.LocalDate

enum class IntervalUnit(val displayName: String, val singularName: String) {
    DAYS("Days", "day"),
    WEEKS("Weeks", "week"),
    MONTHS("Months", "month"),
    YEARS("Years", "year");

    fun format(value: Int): String {
        return if (value == 1) "$value $singularName" else "$value $displayName"
    }
}

enum class MonthlyAnchorMode(val label: String, val description: String) {
    PRESERVE_ORIGINAL_DAY(
        label = "Preserve Original Day (Recommended)",
        description = "Keeps original day (e.g. Jan 31 -> Feb 28 -> Mar 31). Best for salary, billing & regular monthly schedules."
    ),
    CLAMP_AND_CASCADE(
        label = "Clamp and Cascade",
        description = "Adjusts to month length and stays at the clamped day (e.g. Jan 31 -> Feb 28 -> Mar 28)."
    )
}

enum class ThemeMode(val label: String) {
    SYSTEM("Follow System"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class DateFormatOption(val pattern: String, val label: String, val example: String) {
    ISO_DATE("yyyy-MM-dd", "Standard ISO (YYYY-MM-DD)", "2026-10-30"),
    MEDIUM_DATE("MMM d, yyyy", "Month Day, Year", "Oct 30, 2026"),
    FULL_DATE("EEEE, MMMM d, yyyy", "Full Date with Day", "Friday, October 30, 2026"),
    UK_EURO_DATE("dd/MM/yyyy", "Day/Month/Year (DD/MM/YYYY)", "30/10/2026"),
    US_DATE("MM/dd/yyyy", "Month/Day/Year (MM/DD/YYYY)", "10/30/2026")
}

data class DateCycleItem(
    val index: Int,
    val date: LocalDate,
    val daysFromStart: Long,
    val daysFromPrevious: Long
)

data class DateCycleResult(
    val startDate: LocalDate,
    val intervalValue: Int,
    val intervalUnit: IntervalUnit,
    val requestedOccurrences: Int,
    val endDate: LocalDate?,
    val monthlyAnchorMode: MonthlyAnchorMode,
    val dates: List<DateCycleItem>,
    val stoppedByEndDate: Boolean
)

data class DateCycleHistory(
    val id: String,
    val timestamp: Long,
    val title: String,
    val startDateEpochDay: Long,
    val intervalValue: Int,
    val intervalUnit: IntervalUnit,
    val occurrenceCount: Int,
    val endDateEpochDay: Long?,
    val monthlyAnchorMode: MonthlyAnchorMode,
    val generatedDateCount: Int,
    val notes: String? = null
) {
    val startDate: LocalDate get() = LocalDate.ofEpochDay(startDateEpochDay)
    val endDate: LocalDate? get() = endDateEpochDay?.let { LocalDate.ofEpochDay(it) }
}
