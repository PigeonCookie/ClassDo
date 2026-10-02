package com.coursework.tracker.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.coursework.tracker.MainActivity
import com.coursework.tracker.R
import com.coursework.tracker.util.formatDateTime

object Notifier {

    const val CHANNEL_ID = "coursework_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val wantedName = context.getString(R.string.channel_name)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            // 渠道建好之后系统就不允许再改设置了。名字变了（比如应用改名）就删掉重建，
            // 让老安装也能更新——只在名字不一致时做一次，不会反复重置用户的设置。
            if (existing.name?.toString() == wantedName) return
            manager.deleteNotificationChannel(CHANNEL_ID)
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            wantedName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_desc)
            enableVibration(true)
            setShowBadge(true)
            // 锁屏上也把内容显示出来（系统里还要允许锁屏通知）
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun show(
        context: Context,
        assignmentId: String,
        name: String,
        course: String,
        timeKindLabel: String,
        reportAt: Long,
        label: String,
    ) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val detail = buildString {
            if (course.isNotBlank()) append(course).append(" · ")
            append(timeKindLabel).append("时间 ").append(formatDateTime(reportAt))
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (label.isBlank()) name else "$name · $label")
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            // 高优先级 + 闹钟分类，系统才会以「横幅弹窗」的形式弹出来
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // 锁屏上也直接显示标题和内容
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, assignmentId))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId(assignmentId), notification)
        }
    }

    private fun openAppIntent(context: Context, assignmentId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_FOCUS_ID, assignmentId)
        }
        return PendingIntent.getActivity(
            context,
            notificationId(assignmentId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notificationId(assignmentId: String): Int = assignmentId.hashCode() and 0x7FFFFFFF
}
