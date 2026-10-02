package com.coursework.tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.graphics.vector.ImageVector
import com.coursework.tracker.model.TimeKind

/** 截止用旗子，汇报用话筒，一眼能区分 */
val TimeKind.icon: ImageVector
    get() = when (this) {
        TimeKind.DEADLINE -> Icons.Rounded.Flag
        TimeKind.REPORT -> Icons.Rounded.Mic
    }

/** 「截止时间」/「汇报时间」 */
val TimeKind.fullLabel: String
    get() = "${label}时间"
