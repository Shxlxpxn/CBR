package com.example.dip.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import java.util.Locale

object LocaleHelper {
    fun getSavedLanguage(context: Context): String {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("app_language", "ru") ?: "ru"
    }
    fun applyLocale(context: Context): Context {
        val lang = getSavedLanguage(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    // Обертка для Activity
    fun wrap(base: Context): Context {
        return applyLocale(base)
    }
}