package com.example.repository

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aurapad_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_setting"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_APP_LOCK_PIN = "app_lock_pin"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
    }

    var themeSetting: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var appLockPin: String?
        get() = prefs.getString(KEY_APP_LOCK_PIN, null)
        set(value) = prefs.edit().putString(KEY_APP_LOCK_PIN, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, value).apply()
}
