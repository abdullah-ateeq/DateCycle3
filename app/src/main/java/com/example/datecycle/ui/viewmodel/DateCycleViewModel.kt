package com.example.datecycle.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.datecycle.data.calculator.DateCycleCalculator
import com.example.datecycle.data.model.DateFormatOption
import com.example.datecycle.data.model.DateCycleHistory
import com.example.datecycle.data.model.DateCycleResult
import com.example.datecycle.data.model.IntervalUnit
import com.example.datecycle.data.model.MonthlyAnchorMode
import com.example.datecycle.data.model.ThemeMode
import com.example.datecycle.data.repository.DateCycleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

import androidx.lifecycle.viewmodel.CreationExtras

class DateCycleViewModel(
    application: Application,
    private val repository: DateCycleRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, DateCycleRepository(application))

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return DateCycleViewModel(application, DateCycleRepository(application)) as T
            }
        }
    }

    private val _inputState = MutableStateFlow(DateCycleUiState())

    val uiState: StateFlow<DateCycleUiState> = combine(
        _inputState,
        repository.themeModeFlow,
        repository.dateFormatFlow,
        repository.monthlyAnchorModeFlow,
        repository.historyFlow
    ) { currentInput, theme, format, anchor, history ->
        currentInput.copy(
            themeMode = theme,
            dateFormatOption = format,
            monthlyAnchorMode = anchor,
            historyList = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DateCycleUiState()
    )

    fun onStartDateChanged(date: LocalDate) {
        _inputState.update {
            val updated = it.copy(startDate = date, validationError = null)
            // If end date exists and is before new start date, adjust or invalidate
            if (updated.hasEndDate && updated.endDate != null && updated.endDate.isBefore(date)) {
                updated.copy(endDate = date.plusMonths(1))
            } else {
                updated
            }
        }
    }

    fun onRepeatEveryChanged(text: String) {
        val filtered = text.filter { it.isDigit() }.take(4)
        val value = filtered.toIntOrNull() ?: 0
        _inputState.update {
            it.copy(
                repeatEvery = value,
                repeatEveryText = filtered,
                validationError = null
            )
        }
    }

    fun onIntervalUnitChanged(unit: IntervalUnit) {
        _inputState.update { it.copy(intervalUnit = unit, validationError = null) }
    }

    fun onOccurrencesChanged(count: Int) {
        val clamped = count.coerceIn(DateCycleCalculator.MIN_OCCURRENCES, DateCycleCalculator.MAX_OCCURRENCES)
        _inputState.update {
            it.copy(
                occurrences = clamped,
                occurrencesText = clamped.toString(),
                validationError = null
            )
        }
    }

    fun onOccurrencesTextChanged(text: String) {
        val filtered = text.filter { it.isDigit() }.take(3)
        val value = filtered.toIntOrNull() ?: 0
        _inputState.update {
            it.copy(
                occurrences = value,
                occurrencesText = filtered,
                validationError = null
            )
        }
    }

    fun onHasEndDateToggled(enabled: Boolean) {
        _inputState.update {
            if (enabled) {
                val defaultEnd = it.endDate ?: it.startDate.plusMonths(3)
                it.copy(hasEndDate = true, endDate = defaultEnd, validationError = null)
            } else {
                it.copy(hasEndDate = false, validationError = null)
            }
        }
    }

    fun onEndDateChanged(date: LocalDate) {
        _inputState.update { it.copy(endDate = date, validationError = null) }
    }

    fun generateDates(): Boolean {
        val state = uiState.value
        val intervalVal = state.repeatEveryText.toIntOrNull()
        val occurrencesVal = state.occurrencesText.toIntOrNull()
        val effectiveEndDate = if (state.hasEndDate) state.endDate else null

        val validation = DateCycleCalculator.validate(
            startDate = state.startDate,
            intervalValue = intervalVal,
            intervalUnit = state.intervalUnit,
            occurrences = occurrencesVal,
            endDate = effectiveEndDate
        )

        when (validation) {
            is DateCycleCalculator.ValidationResult.Error -> {
                _inputState.update { it.copy(validationError = validation.message) }
                return false
            }
            DateCycleCalculator.ValidationResult.Valid -> {
                val result = DateCycleCalculator.calculate(
                    startDate = state.startDate,
                    intervalValue = intervalVal ?: 1,
                    intervalUnit = state.intervalUnit,
                    occurrences = occurrencesVal ?: 10,
                    endDate = effectiveEndDate,
                    monthlyAnchorMode = state.monthlyAnchorMode
                )
                _inputState.update {
                    it.copy(
                        currentResult = result,
                        validationError = null,
                        repeatEvery = intervalVal ?: 1,
                        occurrences = occurrencesVal ?: 10
                    )
                }
                return true
            }
        }
    }

    fun saveCurrentResultToHistory(customTitle: String? = null, notes: String? = null) {
        val result = uiState.value.currentResult ?: return
        viewModelScope.launch {
            val title = if (!customTitle.isNullOrBlank()) {
                customTitle.trim()
            } else {
                "Every ${result.intervalUnit.format(result.intervalValue)} from ${result.startDate}"
            }

            val historyItem = DateCycleHistory(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                title = title,
                startDateEpochDay = result.startDate.toEpochDay(),
                intervalValue = result.intervalValue,
                intervalUnit = result.intervalUnit,
                occurrenceCount = result.dates.size,
                endDateEpochDay = result.endDate?.toEpochDay(),
                monthlyAnchorMode = result.monthlyAnchorMode,
                generatedDateCount = result.dates.size,
                notes = notes?.takeIf { it.isNotBlank() }
            )
            repository.saveHistory(historyItem)
            _inputState.update { it.copy(userNotification = "Saved cycle to history!") }
        }
    }

    fun loadHistoryItem(item: DateCycleHistory) {
        val result = DateCycleCalculator.calculate(
            startDate = item.startDate,
            intervalValue = item.intervalValue,
            intervalUnit = item.intervalUnit,
            occurrences = item.occurrenceCount,
            endDate = item.endDate,
            monthlyAnchorMode = item.monthlyAnchorMode
        )
        _inputState.update {
            it.copy(
                startDate = item.startDate,
                repeatEvery = item.intervalValue,
                repeatEveryText = item.intervalValue.toString(),
                intervalUnit = item.intervalUnit,
                occurrences = item.occurrenceCount,
                occurrencesText = item.occurrenceCount.toString(),
                hasEndDate = item.endDate != null,
                endDate = item.endDate,
                monthlyAnchorMode = item.monthlyAnchorMode,
                currentResult = result,
                validationError = null,
                userNotification = "Loaded '${item.title}'"
            )
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteHistory(id)
            _inputState.update { it.copy(userNotification = "History item deleted") }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _inputState.update { it.copy(userNotification = "All history cleared") }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDateFormat(option: DateFormatOption) {
        viewModelScope.launch { repository.setDateFormat(option) }
    }

    fun setMonthlyAnchorMode(mode: MonthlyAnchorMode) {
        viewModelScope.launch { repository.setMonthlyAnchorMode(mode) }
    }

    fun dismissNotification() {
        _inputState.update { it.copy(userNotification = null) }
    }

    fun buildExportText(result: DateCycleResult, formatOption: DateFormatOption): String {
        val formatter = DateTimeFormatter.ofPattern(formatOption.pattern)
        val sb = StringBuilder()
        sb.appendLine("DateCycle Repeating Dates")
        sb.appendLine("=========================")
        sb.appendLine("Start Date: ${result.startDate.format(formatter)} (${result.startDate.dayOfWeek})")
        sb.appendLine("Interval: Every ${result.intervalUnit.format(result.intervalValue)}")
        if (result.endDate != null) {
            sb.appendLine("End Date Cutoff: ${result.endDate.format(formatter)}")
        }
        sb.appendLine("Total Dates: ${result.dates.size}")
        sb.appendLine("-------------------------")
        result.dates.forEach { item ->
            val formattedDate = item.date.format(formatter)
            val dayOfWeek = item.date.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() }
            sb.appendLine("#${item.index}. $formattedDate ($dayOfWeek) [+${item.daysFromStart} days]")
        }
        sb.appendLine("-------------------------")
        sb.appendLine("Generated offline with DateCycle")
        return sb.toString()
    }
}
