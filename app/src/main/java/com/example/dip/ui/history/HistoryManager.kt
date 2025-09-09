package com.example.dip.ui.history

import android.content.Context

class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)

    fun addToHistory(code: String) {
        val set = prefs.getStringSet("currency_history", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(code)
        prefs.edit().putStringSet("currency_history", set).apply()
    }

    fun getHistory(): List<String> {
        return prefs.getStringSet("currency_history", emptySet())?.toList() ?: emptyList()
    }

    fun clearHistory() {
        prefs.edit().remove("currency_history").apply()
    }
}