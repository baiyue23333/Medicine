package com.baiyue.medicine.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val defaultWarningDays: Int = 3,
    val keepAlive: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderAdvanceMinutes: Int = 0,
    val reminderMessage: String = "",
    /** 错过服药时段后是否提醒补签 */
    val missedReminderEnabled: Boolean = true,
    /** 是否在列表/打卡页显示药品图片 */
    val showImage: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DEFAULT_WARNING_DAYS = intPreferencesKey("default_warning_days")
        val KEEP_ALIVE = booleanPreferencesKey("keep_alive")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_ADVANCE_MINUTES = intPreferencesKey("reminder_advance_minutes")
        val REMINDER_MESSAGE = stringPreferencesKey("reminder_message")
        val MISSED_REMINDER_ENABLED = booleanPreferencesKey("missed_reminder_enabled")
        val SHOW_IMAGE = booleanPreferencesKey("show_image")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            defaultWarningDays = p[Keys.DEFAULT_WARNING_DAYS] ?: 3,
            keepAlive = p[Keys.KEEP_ALIVE] ?: true,
            reminderEnabled = p[Keys.REMINDER_ENABLED] ?: true,
            reminderAdvanceMinutes = p[Keys.REMINDER_ADVANCE_MINUTES] ?: 0,
            reminderMessage = p[Keys.REMINDER_MESSAGE] ?: "",
            missedReminderEnabled = p[Keys.MISSED_REMINDER_ENABLED] ?: true,
            showImage = p[Keys.SHOW_IMAGE] ?: true
        )
    }

    suspend fun setDefaultWarningDays(days: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_WARNING_DAYS] = days }
    }

    suspend fun setKeepAlive(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_ALIVE] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderAdvanceMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REMINDER_ADVANCE_MINUTES] = minutes }
    }

    suspend fun setReminderMessage(message: String) {
        context.dataStore.edit { it[Keys.REMINDER_MESSAGE] = message }
    }

    suspend fun setMissedReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MISSED_REMINDER_ENABLED] = enabled }
    }

    suspend fun setShowImage(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_IMAGE] = enabled }
    }
}