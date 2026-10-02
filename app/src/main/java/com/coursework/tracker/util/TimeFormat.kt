package com.coursework.tracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val DATE = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private val DATE_FULL = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
private val DATE_WEEK = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)
private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
private val MONTH = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

/**
 * 同一年的日期省略年份（「9月15日」），跨年才带上（「2099年9月15日」）。
 * 年份可选到 2099，不这么处理的话远期日期会看不出来是哪一年。
 */
private fun formatDay(dt: LocalDateTime): String =
    dt.format(if (dt.year == LocalDate.now().year) DATE else DATE_FULL)

private val WEEKDAYS = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun LocalDateTime.toEpochMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate = toLocalDateTime().toLocalDate()

fun LocalDate.toEpochMillisAtStartOfDay(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatDate(millis: Long): String = formatDay(millis.toLocalDateTime())

fun formatDateTime(millis: Long): String {
    val dt = millis.toLocalDateTime()
    return "${formatDay(dt)} ${dt.format(TIME)}"
}

fun formatMonth(date: LocalDate): String = date.format(MONTH)

fun formatClock(dateTime: LocalDateTime): String = dateTime.format(TIME)

fun weekdayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "今天"
    today.plusDays(1) -> "明天"
    today.minusDays(1) -> "昨天"
    else -> WEEKDAYS[date.dayOfWeek.value - 1]
}

fun formatDateWithWeek(millis: Long): String = millis.toLocalDateTime().format(DATE_WEEK)

/** 「还剩 2 天 3 小时」/「已逾期 5 小时」 */
fun formatRemaining(reportAtMillis: Long, now: Long): String {
    val diff = reportAtMillis - now
    val mins = abs(diff) / 60_000L
    val days = mins / (60 * 24)
    val hours = (mins % (60 * 24)) / 60
    val rest = mins % 60

    val span = when {
        days > 0 -> "$days 天 $hours 小时"
        hours > 0 -> "$hours 小时 $rest 分钟"
        else -> "$rest 分钟"
    }
    return if (diff < 0) "已逾期 $span" else "还剩 $span"
}

/** 日期差（自然日），今天为 0 */
fun daysUntil(date: LocalDate, today: LocalDate): Long =
    date.toEpochDay() - today.toEpochDay()
