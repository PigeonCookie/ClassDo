package com.coursework.tracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coursework.tracker.model.Assignment
import com.coursework.tracker.ui.theme.brandContentColor
import com.coursework.tracker.util.formatDateTime
import com.coursework.tracker.util.formatRemaining

/**
 * 条状矩形卡片 = 这条作业的「简介」：
 * 项目名称、持续时间、汇报时间、时间进度各一行，名称超出用省略号。
 * 想看完整内容（备注、照片）点一下进详情。
 */
@Composable
fun AssignmentCard(
    item: Assignment,
    now: Long,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = urgencyOf(item, now).color
    val shape = RoundedCornerShape(20.dp)
    val progress by animateFloatAsState(
        targetValue = if (item.done) 1f else item.remainingRatioAt(now),
        label = "cardProgress",
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().widthIn(max = 660.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // 左侧状态色条
                    val barWidth = 4.dp.toPx()
                    val inset = 12.dp.toPx()
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(0f, inset),
                        size = Size(barWidth, (size.height - inset * 2).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(barWidth / 2f),
                    )
                }
                .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DoneToggle(done = item.done, accent = accent, onClick = onToggleDone)

                Spacer(Modifier.width(6.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.name.ifBlank { "未命名作业" },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.done) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.course.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.course,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (item.photos.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    PhotoCountBadge(count = item.photos.size)
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "查看详情",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(11.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(
                    icon = item.timeKind.icon,
                    text = "${item.timeKind.label} ${formatDateTime(item.reportAt)}",
                    modifier = Modifier.weight(1f, fill = false),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.copy(alpha = 0.16f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(accent)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (item.done) "已完成" else formatRemaining(item.reportAt, now),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DoneToggle(done: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (done) "标记为未完成" else "标记为已完成",
            tint = accent,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun PhotoCountBadge(count: Int) {
    val brand = brandContentColor()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brand.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = "$count 张照片",
            tint = brand,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = brand,
        )
    }
}

/** 详情页右上角的圆形操作按钮（编辑 / 删除） */
@Composable
fun ActionIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun MetaChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
