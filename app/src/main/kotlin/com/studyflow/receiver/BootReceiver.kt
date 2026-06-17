package com.studyflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.studyflow.work.ReminderSchedulingWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Reschedule reminders after device reboot
        val request = OneTimeWorkRequestBuilder<ReminderSchedulingWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
