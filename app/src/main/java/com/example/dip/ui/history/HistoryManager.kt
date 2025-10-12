package com.example.dip.ui.history

import android.content.Context

class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)

    fun addToHistory(record: String) {
        val current = getHistory().toMutableList()
        if (!current.contains(record)) {
            current.add(record)
            saveHistory(current)
        }
    }

    fun getHistory(): List<String> {
        val set = prefs.getStringSet("history_set", emptySet()) ?: emptySet()
        return set.toList()
    }

    fun clearHistory() {
        prefs.edit().remove("history_set").apply()
    }

    fun removeFromHistory(item: String) {
        val current = getHistory().toMutableList()
        if (current.remove(item)) {
            saveHistory(current)
        }
    }

    private fun saveHistory(list: List<String>) {
        prefs.edit().putStringSet("history_set", list.toSet()).apply()
    }
}