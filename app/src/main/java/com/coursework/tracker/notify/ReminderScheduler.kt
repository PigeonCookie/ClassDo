package com.coursework.tracker.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.coursework.tracker.model.Assignment
import com.coursework.tracker.model.TimeKind

/**
 * 给每条作业安排「提前 1 天 / 提前 1 小时 / 到点」三次本地提醒。
 *
 * Android 12+ 精确闹钟需要用户授权，未授权时自动降级为非精确闹钟，
 * 保证任何机型和任何权限状态下都能收到提醒。
 */
object ReminderScheduler {

    const val EXTRA_ID = "extra_id"
    const val EXTRA_NAME = "extra_name"
    const val EXTRA_COURSE = "extra_course"
    const val EXTRA_REPORT_AT = "extra_report_at"
    const val EXTRA_TIME_KIND = "extra_time_kind"
    const val EXTRA_LABEL = "extra_label"

    private const val HOUR = 60 * 60 * 1000L
    private const val DAY = 24 * HOUR

    /** 提前提醒的时间点，到点那一条另外加在末尾 */
    private val LEAD_TIMES = listOf(DAY to "1 天", HOUR to "1 小时")

    /** 每条作业一共排几个闹钟（提前两条 + 到点一条），取消时按同样的索引撤 */
    private const val CHECKPOINT_COUNT = 3

    // 提醒文案跟着作业当初设的类型走：设的「截止」就说截止，设的「汇报」就说汇报，
    // 不能写死——不然设成截止的作业会收到「现在就要汇报啦」，前言不搭后语
    private fun TimeKind.dueLabel(): String = when (this) {
        TimeKind.DEADLINE -> "现在就到截止时间啦"
        TimeKind.REPORT -> "现在就要汇报啦"
    }

    private fun TimeKind.leadLabel(span: String): String = when (this) {
        TimeKind.DEADLINE -> "距截止还有 $span"
        TimeKind.REPORT -> "距汇报还有 $span"
    }

    fun syncAll(context: Context, items: List<Assignment>) {
        items.forEach { sync(context, it) }
    }

    fun sync(context: Context, item: Assignment) {
        cancel(context, item)
        if (item.done) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val exact = canScheduleExact(alarmManager)

        val checkpoints: List<Pair<Long, String>> =
            LEAD_TIMES.map { (lead, span) -> lead to item.timeKind.leadLabel(span) } +
                (0L to item.timeKind.dueLabel())

        checkpoints.forEachIndexed { index, (leadTime, label) ->
            val triggerAt = item.reportAt - leadTime
            if (triggerAt <= now) return@forEachIndexed
            val pending = pendingIntent(context, item, index, label)
            runCatching {
                if (exact) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            }
        }
    }

    fun cancel(context: Context, item: Assignment) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        repeat(CHECKPOINT_COUNT) { index ->
            runCatching { alarmManager.cancel(pendingIntent(context, item, index, null)) }
        }
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun pendingIntent(
        context: Context,
        item: Assignment,
        index: Int,
        label: String?,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            // data 唯一，保证不同作业、不同提醒节点的 PendingIntent 不会互相覆盖
            data = Uri.parse("coursework://reminder/${item.id}/$index")
            putExtra(EXTRA_ID, item.id)
            putExtra(EXTRA_NAME, item.name)
            putExtra(EXTRA_COURSE, item.course)
            putExtra(EXTRA_REPORT_AT, item.reportAt)
            putExtra(EXTRA_TIME_KIND, item.timeKind.label)
            putExtra(EXTRA_LABEL, label)
        }
        var flags = PendingIntent.FLAG_IMMUTABLE
        if (label != null) flags = flags or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, index, intent, flags)
    }
}
