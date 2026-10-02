package com.coursework.tracker.model

import kotlinx.serialization.Serializable
import java.util.UUID

private const val SECOND_MILLIS = 1_000L
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

/** 这个时间是「截止」还是「汇报」，由用户每条自己选 */
@Serializable
enum class TimeKind(val label: String) {
    DEADLINE("截止"),
    REPORT("汇报"),
}

/**
 * 一条课程作业记录。
 *
 * @param name 作业/项目名称
 * @param course 课程名称（可选，用于区分同一门课的多个作业）
 * @param timeKind 这个时间是「截止」还是「汇报」
 * @param reportAt 截止/汇报时间，epoch millis
 * @param createdAt 创建时间，用于计算时间进度条
 * @param note 备注
 * @param done 是否已完成
 * @param photos 照片文件名列表，实际文件存在 filesDir/photos/ 下
 */
@Serializable
data class Assignment(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val course: String = "",
    val timeKind: TimeKind = TimeKind.DEADLINE,
    val reportAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val done: Boolean = false,
    val photos: List<String> = emptyList(),
) {

    /** 距离汇报时间还剩多少毫秒，负数表示已逾期 */
    fun millisLeft(now: Long): Long = reportAt - now

    fun isOverdue(now: Long): Boolean = !done && reportAt < now

    /**
     * 剩余时间占总时长的比例：刚记录时接近 1，到点归 0。
     *
     * 公式是「当前到截止的剩余时间 ÷ 记录时刻到截止的总时间」，单位按总跨度分档，
     * 跨度越小单位越细，进度条才走得动：
     *  · 跨度 < 1 小时   → 按秒
     *  · 跨度 < 3 天     → 按分钟
     *  · 跨度 ≥ 3 天     → 按小时
     *
     * 跨度 = reportAt − createdAt，两个时间戳都是固定的，所以单位只跟「记录时刻到截止」
     * 这段跨度有关、不随时间漂移；改了截止时间，跨度变了，单位自然跟着换一档。
     */
    fun remainingRatioAt(now: Long): Float {
        val totalMillis = reportAt - createdAt
        if (totalMillis <= 0L) return 0f

        val unit = when {
            totalMillis < HOUR_MILLIS -> SECOND_MILLIS
            totalMillis < 3 * DAY_MILLIS -> MINUTE_MILLIS
            else -> HOUR_MILLIS
        }
        val total = totalMillis / unit
        if (total <= 0L) return 0f

        val left = (reportAt - now).coerceAtLeast(0L) / unit
        return (left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}
