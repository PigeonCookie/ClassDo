package com.coursework.tracker

import android.app.Application
import com.coursework.tracker.data.DraftStore
import com.coursework.tracker.notify.Notifier

class HomeworkApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 通知渠道越早创建越好，避免第一条提醒到达时才建渠道
        Notifier.ensureChannel(this)
        // 上次没填完的草稿如果已经超时，启动时就清掉（含它带进来的照片）
        DraftStore.discardIfExpired(this)
    }
}
