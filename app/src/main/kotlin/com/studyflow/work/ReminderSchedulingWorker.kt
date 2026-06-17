package com.studyflow.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studyflow.data.datastore.UserPreferencesDataStore
import com.studyflow.receiver.AlarmReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

/**
 * Reads preferences and (re)schedules all reminder alarms via AlarmManager.
 * Runs after boot and whenever settings change.
 */
class ReminderSchedulingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferencesDataStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (prefs.reminderDailyEnabled) {
            val t = prefs.reminderDailyTimeMinutes
            scheduleDailyReminder(ctx, t, "Time to study!", "You haven't started a session yet today.")
        }
        if (prefs.reminderStreakEnabled) {
            scheduleDailyReminder(ctx, 22 * 60, "Streak at risk!", "Study at least 15 min today to keep your streak alive.")
        }
        return Result.success()
    }

    private fun scheduleDailyReminder(ctx: Context, atMinuteOfDay: Int, title: String, text: String) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, atMinuteOfDay / 60)
            set(Calendar.MINUTE, atMinuteOfDay % 60)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_TEXT, text)
        }
        val pi = PendingIntent.getBroadcast(ctx, atMinuteOfDay, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi,
        )
    }
}
