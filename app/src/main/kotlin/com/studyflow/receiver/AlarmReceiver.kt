package com.studyflow.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.studyflow.R
import com.studyflow.service.StudyTimerService
import dagger.hilt.android.AndroidEntryPoint

/** Fires when an AlarmManager-scheduled reminder (streak-at-risk, daily nudge) goes off. */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "StudyFlow"
        val text = intent.getStringExtra(EXTRA_TEXT) ?: "Time to study!"
        showReminder(context, title, text)
    }

    private fun showReminder(context: Context, title: String, text: String) {
        StudyTimerService.ensureChannels(context)
        val notif = NotificationCompat.Builder(context, StudyTimerService.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
    }
}
