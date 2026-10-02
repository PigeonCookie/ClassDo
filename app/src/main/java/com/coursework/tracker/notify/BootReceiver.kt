package com.coursework.tracker.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coursework.tracker.data.AssignmentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 手机重启或应用更新后，系统会清空所有闹钟，这里重新排一遍。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val items = AssignmentRepository(appContext).load()
                ReminderScheduler.syncAll(appContext, items)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
