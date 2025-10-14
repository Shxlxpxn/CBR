package com.example.dip.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.dip.R
import com.example.dip.utils.LanguageManager

class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<ListPreference>("base_currency")
            ?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        findPreference<ListPreference>("page_size")
            ?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        findPreference<SwitchPreferenceCompat>("dark_theme")

        findPreference<ListPreference>("language")
            ?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "dark_theme" -> {
                val isDark = sharedPreferences?.getBoolean("dark_theme", false) ?: false
                AppCompatDelegate.setDefaultNightMode(
                    if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                requireActivity().recreate()
            }

            "language" -> {
                val lang = sharedPreferences?.getString("language", "ru") ?: "ru"
                LanguageManager.setLocale(requireContext(), lang)
                requireActivity().recreate()
            }
        }
    }
}