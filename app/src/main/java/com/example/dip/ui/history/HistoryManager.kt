package com.example.dip.ui.history

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)
    private val key = "history_list"
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<HistoryItem>>() {}.type

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            gson.fromJson<List<HistoryItem>>(json, typeToken) ?: emptyList()
        } catch (e: Exception) {
            prefs.edit().remove(key).apply()
            emptyList()
        }
    }

    fun addToHistory(item: HistoryItem) {
        val list = getHistory().toMutableList()
        list.removeAll { it.baseCurrency == item.baseCurrency && it.targetCurrency == item.targetCurrency }
        list.add(item)
        saveHistory(list)
    }

    fun removeFromHistory(item: HistoryItem) {
        val list = getHistory().toMutableList()
        val removed = list.removeAll { it.baseCurrency == item.baseCurrency && it.targetCurrency == item.targetCurrency }
        if (removed) saveHistory(list)
    }

    fun clearHistory() {
        prefs.edit().remove(key).apply()
    }

    private fun saveHistory(list: List<HistoryItem>) {
        try {
            val json = gson.toJson(list)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
        }
    }
}