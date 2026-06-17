package com.studyflow.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "studyflow_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val dailyGoalMinutes = intPreferencesKey("daily_goal_minutes")
        val pomodoroWork = intPreferencesKey("pomodoro_work_minutes")
        val pomodoroBreak = intPreferencesKey("pomodoro_break_minutes")
        val defaultMode = stringPreferencesKey("default_mode")
        val theme = stringPreferencesKey("theme")            // "system" | "light" | "dark"
        val firstDayOfWeek = stringPreferencesKey("first_day_of_week")  // "mon" | "sun"
        val reminderDailyEnabled = booleanPreferencesKey("reminder_daily_enabled")
        val reminderDailyTimeMinutes = intPreferencesKey("reminder_daily_time_minutes")  // minutes from 00:00
        val reminderGoalEnabled = booleanPreferencesKey("reminder_goal_enabled")
        val reminderStreakEnabled = booleanPreferencesKey("reminder_streak_enabled")
        val maxBreakMinutes = intPreferencesKey("max_break_minutes")
        val dndStartMinutes = intPreferencesKey("dnd_start_minutes")  // e.g. 23*60
        val dndEndMinutes = intPreferencesKey("dnd_end_minutes")      // e.g. 7*60
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingComplete] ?: false }
    val dailyGoalMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.dailyGoalMinutes] ?: 120 }
    val pomodoroWork: Flow<Int> = context.dataStore.data.map { it[Keys.pomodoroWork] ?: 25 }
    val pomodoroBreak: Flow<Int> = context.dataStore.data.map { it[Keys.pomodoroBreak] ?: 5 }
    val defaultMode: Flow<String> = context.dataStore.data.map { it[Keys.defaultMode] ?: "theory" }
    val theme: Flow<String> = context.dataStore.data.map { it[Keys.theme] ?: "system" }
    val firstDayOfWeek: Flow<String> = context.dataStore.data.map { it[Keys.firstDayOfWeek] ?: "mon" }
    val reminderDailyEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.reminderDailyEnabled] ?: true }
    val reminderDailyTimeMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.reminderDailyTimeMinutes] ?: 19 * 60 }
    val reminderGoalEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.reminderGoalEnabled] ?: true }
    val reminderStreakEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.reminderStreakEnabled] ?: true }
    val maxBreakMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.maxBreakMinutes] ?: 15 }
    val dndStartMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.dndStartMinutes] ?: 23 * 60 }
    val dndEndMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.dndEndMinutes] ?: 7 * 60 }

    suspend fun setOnboardingComplete(v: Boolean) = context.dataStore.edit { it[Keys.onboardingComplete] = v }
    suspend fun setDailyGoalMinutes(v: Int) = context.dataStore.edit { it[Keys.dailyGoalMinutes] = v }
    suspend fun setPomodoroWork(v: Int) = context.dataStore.edit { it[Keys.pomodoroWork] = v }
    suspend fun setPomodoroBreak(v: Int) = context.dataStore.edit { it[Keys.pomodoroBreak] = v }
    suspend fun setDefaultMode(v: String) = context.dataStore.edit { it[Keys.defaultMode] = v }
    suspend fun setTheme(v: String) = context.dataStore.edit { it[Keys.theme] = v }
    suspend fun setFirstDayOfWeek(v: String) = context.dataStore.edit { it[Keys.firstDayOfWeek] = v }
    suspend fun setReminderDailyEnabled(v: Boolean) = context.dataStore.edit { it[Keys.reminderDailyEnabled] = v }
    suspend fun setReminderDailyTime(v: Int) = context.dataStore.edit { it[Keys.reminderDailyTimeMinutes] = v }
    suspend fun setReminderGoalEnabled(v: Boolean) = context.dataStore.edit { it[Keys.reminderGoalEnabled] = v }
    suspend fun setReminderStreakEnabled(v: Boolean) = context.dataStore.edit { it[Keys.reminderStreakEnabled] = v }
    suspend fun setMaxBreakMinutes(v: Int) = context.dataStore.edit { it[Keys.maxBreakMinutes] = v }
    suspend fun setDnd(start: Int, end: Int) = context.dataStore.edit {
        it[Keys.dndStartMinutes] = start
        it[Keys.dndEndMinutes] = end
    }
}
