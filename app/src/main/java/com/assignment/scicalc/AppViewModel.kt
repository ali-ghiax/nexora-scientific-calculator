package com.assignment.scicalc

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val id: Long,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val engine = CalculatorEngine()

    private val _history = MutableLiveData<List<HistoryEntry>>(emptyList())
    val history: LiveData<List<HistoryEntry>> = _history

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var scientificEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCI, true)
        set(value) = prefs.edit().putBoolean(KEY_SCI, value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    var calcExpression: String = ""
    var calcResult: String = "0"
    var calcEvaluated: Boolean = false

    var degreeMode: Boolean
        get() = prefs.getBoolean(KEY_DEG, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DEG, value).apply()
            engine.degreeMode = value
        }

    init {
        engine.degreeMode = degreeMode
        _history.value = loadHistory()
    }

    fun addHistory(expression: String, result: String) {
        if (expression.isBlank() || engine.isError(result)) return
        val next = listOf(HistoryEntry(System.currentTimeMillis(), expression, result)) +
            (_history.value.orEmpty()).filterNot { it.expression == expression && it.result == result }
        saveHistory(next.take(80))
    }

    fun deleteHistory(id: Long) {
        saveHistory(_history.value.orEmpty().filterNot { it.id == id })
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }

    private fun loadHistory(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        HistoryEntry(
                            id = obj.getLong("id"),
                            expression = obj.getString("expression"),
                            result = obj.getString("result"),
                            timestamp = obj.optLong("timestamp")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(items: List<HistoryEntry>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("expression", item.expression)
                    .put("result", item.result)
                    .put("timestamp", item.timestamp)
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
        _history.value = items
    }

    companion object {
        private const val PREFS = "nexora_prefs"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_SCI = "scientific"
        private const val KEY_DARK = "dark"
        private const val KEY_DEG = "degree"
        private const val KEY_HISTORY = "history"
    }
}
