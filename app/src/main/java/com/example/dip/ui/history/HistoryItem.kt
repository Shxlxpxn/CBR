package com.example.dip.ui.history

data class HistoryItem(
    val baseCurrency: String,
    val targetCurrency: String,
    val timestamp: Long = System.currentTimeMillis()
)