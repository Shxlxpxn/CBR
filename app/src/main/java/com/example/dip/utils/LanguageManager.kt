package com.example.dip.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import java.util.Locale.setDefault


object LanguageManager {
    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}