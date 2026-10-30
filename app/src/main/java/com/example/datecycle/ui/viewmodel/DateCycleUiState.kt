package com.example.datecycle.ui.viewmodel

import com.example.datecycle.data.model.DateFormatOption
import com.example.datecycle.data.model.DateCycleHistory
import com.example.datecycle.data.model.DateCycleResult
import com.example.datecycle.data.model.IntervalUnit
import com.example.datecycle.data.model.MonthlyAnchorMode
import com.example.datecycle.data.model.ThemeMode
import java.time.LocalDate

data class DateCycleUiState(
    val startDate: LocalDate = LocalDate.now(),
    val repeatEvery: Int = 1,
    val repeatEveryText: String = "1",
    val intervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val occurrences: Int = 10,
    val occurrencesText: String = "10",
    val hasEndDate: Boolean = false,
    val endDate: LocalDate? = null,
    val validationError: String? = null,
    val currentResult: DateCycleResult? = null,
    val historyList: List<DateCycleHistory> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dateFormatOption: DateFormatOption = DateFormatOption.MEDIUM_DATE,
    val monthlyAnchorMode: MonthlyAnchorMode = MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY,
    val userNotification: String? = null
)
