package com.studyflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyflow.service.StudyTimerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val svc = Intent(context, StudyTimerService::class.java)
        when (intent.action) {
            ACTION_STOP -> {
                svc.action = StudyTimerService.ACTION_STOP
                context.startService(svc)
            }
            ACTION_BREAK -> {
                svc.action = StudyTimerService.ACTION_BREAK
                svc.putExtra(StudyTimerService.EXTRA_BREAK_MS, 5 * 60 * 1000L)
                context.startService(svc)
            }
            ACTION_RESUME -> {
                svc.action = StudyTimerService.ACTION_RESUME
                context.startService(svc)
            }
        }
    }
    companion object {
        const val ACTION_STOP = "com.studyflow.notif.STOP"
        const val ACTION_BREAK = "com.studyflow.notif.BREAK"
        const val ACTION_RESUME = "com.studyflow.notif.RESUME"
    }
}
