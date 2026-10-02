package com.coursework.tracker.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(ReminderScheduler.EXTRA_NAME) ?: return
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID).orEmpty()
        val course = intent.getStringExtra(ReminderScheduler.EXTRA_COURSE).orEmpty()
        val reportAt = intent.getLongExtra(ReminderScheduler.EXTRA_REPORT_AT, 0L)
        val timeKindLabel = intent.getStringExtra(ReminderScheduler.EXTRA_TIME_KIND).orEmpty()
        val label = intent.getStringExtra(ReminderScheduler.EXTRA_LABEL).orEmpty()

        Notifier.show(
            context = context.applicationContext,
            assignmentId = id,
            name = name,
            course = course,
            timeKindLabel = timeKindLabel,
            reportAt = reportAt,
            label = label,
        )
    }
}
