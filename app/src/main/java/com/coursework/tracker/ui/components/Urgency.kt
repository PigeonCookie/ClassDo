package com.coursework.tracker.ui.components

import androidx.compose.ui.graphics.Color
import com.coursework.tracker.model.Assignment
import com.coursework.tracker.ui.theme.StatusDone
import com.coursework.tracker.ui.theme.StatusNormal
import com.coursework.tracker.ui.theme.StatusOverdue
import com.coursework.tracker.ui.theme.StatusSoon
import com.coursework.tracker.ui.theme.StatusToday
import com.coursework.tracker.util.daysUntil
import com.coursework.tracker.util.toLocalDate

/** 作业的紧急程度，决定卡片左侧色条、进度条和提示文字的颜色 */
enum class Urgency { OVERDUE, TODAY, SOON, NORMAL, DONE }

fun urgencyOf(item: Assignment, now: Long): Urgency {
    if (item.done) return Urgency.DONE
    if (item.reportAt < now) return Urgency.OVERDUE
    val today = now.toLocalDate()
    return when {
        daysUntil(item.reportAt.toLocalDate(), today) <= 0L -> Urgency.TODAY
        daysUntil(item.reportAt.toLocalDate(), today) <= 3L -> Urgency.SOON
        else -> Urgency.NORMAL
    }
}

val Urgency.color: Color
    get() = when (this) {
        Urgency.OVERDUE -> StatusOverdue
        Urgency.TODAY -> StatusToday
        Urgency.SOON -> StatusSoon
        Urgency.NORMAL -> StatusNormal
        Urgency.DONE -> StatusDone
    }

val Urgency.label: String
    get() = when (this) {
        Urgency.OVERDUE -> "已逾期"
        Urgency.TODAY -> "今天到期"
        Urgency.SOON -> "3 天内"
        Urgency.NORMAL -> "进行中"
        Urgency.DONE -> "已完成"
    }

/** 日期条上用来画小圆点的颜色 */
fun markerColors(items: List<Assignment>, now: Long): List<Color> =
    items.sortedBy { it.reportAt }.take(3).map { urgencyOf(it, now).color }
