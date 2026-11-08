package com.example.triduofit.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("triduofit_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TTS_ENGINE = "tts_engine" // "google" ou "openai"
        private const val KEY_GOOGLE_API_KEY = "google_api_key"
        private const val KEY_GOOGLE_VOICE = "google_voice"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_VOICE = "openai_voice"
        private const val KEY_TECHNO_ENABLED = "techno_enabled"
        private const val KEY_TECHNO_BPM = "techno_bpm"
        private const val KEY_USER_STREAK = "user_streak"
        private const val KEY_USER_XP = "user_xp"
        private const val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }

    fun isDailyReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, true)
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, enabled).apply()
    }

    fun getReminderHour(): Int {
        return prefs.getInt(KEY_REMINDER_HOUR, 9)
    }

    fun setReminderHour(hour: Int) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, hour).apply()
    }

    fun getReminderMinute(): Int {
        return prefs.getInt(KEY_REMINDER_MINUTE, 0)
    }

    fun setReminderMinute(minute: Int) {
        prefs.edit().putInt(KEY_REMINDER_MINUTE, minute).apply()
    }

    fun getTtsEngine(): String {
        return prefs.getString(KEY_TTS_ENGINE, "google") ?: "google"
    }

    fun setTtsEngine(engine: String) {
        prefs.edit().putString(KEY_TTS_ENGINE, engine).apply()
    }

    fun getGoogleApiKey(): String {
        return prefs.getString(KEY_GOOGLE_API_KEY, "") ?: ""
    }

    fun setGoogleApiKey(key: String) {
        prefs.edit().putString(KEY_GOOGLE_API_KEY, key.trim()).apply()
    }

    fun getGoogleVoice(): String {
        return prefs.getString(KEY_GOOGLE_VOICE, "fr-FR-Journey-D") ?: "fr-FR-Journey-D"
    }

    fun setGoogleVoice(voice: String) {
        prefs.edit().putString(KEY_GOOGLE_VOICE, voice).apply()
    }

    fun getOpenAiApiKey(): String {
        return prefs.getString(KEY_OPENAI_API_KEY, "") ?: ""
    }

    fun setOpenAiApiKey(key: String) {
        prefs.edit().putString(KEY_OPENAI_API_KEY, key.trim()).apply()
    }

    fun getSelectedVoice(): String {
        return prefs.getString(KEY_OPENAI_VOICE, "onyx") ?: "onyx"
    }

    fun setSelectedVoice(voice: String) {
        prefs.edit().putString(KEY_OPENAI_VOICE, voice).apply()
    }

    fun isTechnoEnabled(): Boolean {
        return prefs.getBoolean(KEY_TECHNO_ENABLED, true)
    }

    fun setTechnoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TECHNO_ENABLED, enabled).apply()
    }

    fun getTechnoBpm(): Int {
        return prefs.getInt(KEY_TECHNO_BPM, 135)
    }

    fun setTechnoBpm(bpm: Int) {
        prefs.edit().putInt(KEY_TECHNO_BPM, bpm).apply()
    }

    fun getUserStreak(): Int {
        return prefs.getInt(KEY_USER_STREAK, 3) // Valeur initiale motivante
    }

    fun incrementStreak() {
        val current = getUserStreak()
        prefs.edit().putInt(KEY_USER_STREAK, current + 1).apply()
    }

    fun getUserXp(): Int {
        return prefs.getInt(KEY_USER_XP, 240)
    }

    fun addXp(amount: Int) {
        val current = getUserXp()
        prefs.edit().putInt(KEY_USER_XP, current + amount).apply()
    }
}
