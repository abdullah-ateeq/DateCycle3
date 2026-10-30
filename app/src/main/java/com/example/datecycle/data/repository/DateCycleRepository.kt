package com.example.datecycle.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.datecycle.data.model.DateFormatOption
import com.example.datecycle.data.model.DateCycleHistory
import com.example.datecycle.data.model.IntervalUnit
import com.example.datecycle.data.model.MonthlyAnchorMode
import com.example.datecycle.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "datecycle_prefs")

class DateCycleRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val MONTHLY_ANCHOR = stringPreferencesKey("monthly_anchor")
        val HISTORY_JSON = stringPreferencesKey("history_json")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val dateFormatFlow: Flow<DateFormatOption> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.DATE_FORMAT] ?: DateFormatOption.MEDIUM_DATE.name
        try {
            DateFormatOption.valueOf(raw)
        } catch (_: Exception) {
            DateFormatOption.MEDIUM_DATE
        }
    }

    val monthlyAnchorModeFlow: Flow<MonthlyAnchorMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.MONTHLY_ANCHOR] ?: MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY.name
        try {
            MonthlyAnchorMode.valueOf(raw)
        } catch (_: Exception) {
            MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY
        }
    }

    val historyFlow: Flow<List<DateCycleHistory>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
        deserializeHistory(jsonString)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDateFormat(option: DateFormatOption) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATE_FORMAT] = option.name
        }
    }

    suspend fun setMonthlyAnchorMode(mode: MonthlyAnchorMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_ANCHOR] = mode.name
        }
    }

    suspend fun saveHistory(item: DateCycleHistory) {
        context.dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
            val currentList = deserializeHistory(jsonString).toMutableList()
            // Add at top (newest first), limit to 100 history items
            currentList.removeAll { it.id == item.id }
            currentList.add(0, item)
            val trimmed = if (currentList.size > 100) currentList.take(100) else currentList
            preferences[PreferencesKeys.HISTORY_JSON] = serializeHistory(trimmed)
        }
    }

    suspend fun deleteHistory(id: String) {
        context.dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.HISTORY_JSON] ?: "[]"
            val currentList = deserializeHistory(jsonString).filterNot { it.id == id }
            preferences[PreferencesKeys.HISTORY_JSON] = serializeHistory(currentList)
        }
    }

    suspend fun clearAllHistory() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HISTORY_JSON] = "[]"
        }
    }

    private fun serializeHistory(list: List<DateCycleHistory>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("title", item.title)
                put("startDateEpochDay", item.startDateEpochDay)
                put("intervalValue", item.intervalValue)
                put("intervalUnit", item.intervalUnit.name)
                put("occurrenceCount", item.occurrenceCount)
                if (item.endDateEpochDay != null) {
                    put("endDateEpochDay", item.endDateEpochDay)
                }
                put("monthlyAnchorMode", item.monthlyAnchorMode.name)
                put("generatedDateCount", item.generatedDateCount)
                if (item.notes != null) {
                    put("notes", item.notes)
                }
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeHistory(jsonString: String): List<DateCycleHistory> {
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<DateCycleHistory>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val unitName = obj.optString("intervalUnit", IntervalUnit.DAYS.name)
                val unit = try {
                    IntervalUnit.valueOf(unitName)
                } catch (_: Exception) {
                    IntervalUnit.DAYS
                }

                val anchorName = obj.optString("monthlyAnchorMode", MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY.name)
                val anchor = try {
                    MonthlyAnchorMode.valueOf(anchorName)
                } catch (_: Exception) {
                    MonthlyAnchorMode.PRESERVE_ORIGINAL_DAY
                }

                val endDateEpoch = if (obj.has("endDateEpochDay") && !obj.isNull("endDateEpochDay")) {
                    obj.getLong("endDateEpochDay")
                } else null

                val notes = if (obj.has("notes") && !obj.isNull("notes")) {
                    obj.getString("notes")
                } else null

                list.add(
                    DateCycleHistory(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        title = obj.getString("title"),
                        startDateEpochDay = obj.getLong("startDateEpochDay"),
                        intervalValue = obj.getInt("intervalValue"),
                        intervalUnit = unit,
                        occurrenceCount = obj.getInt("occurrenceCount"),
                        endDateEpochDay = endDateEpoch,
                        monthlyAnchorMode = anchor,
                        generatedDateCount = obj.optInt("generatedDateCount", obj.getInt("occurrenceCount")),
                        notes = notes
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
