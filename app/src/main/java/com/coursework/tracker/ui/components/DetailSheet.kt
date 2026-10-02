package com.coursework.tracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coursework.tracker.model.Assignment
import com.coursework.tracker.ui.theme.brandContentColor
import com.coursework.tracker.util.formatDateTime
import com.coursework.tracker.util.formatRemaining
import kotlin.math.roundToInt

/**
 * 作业详情：卡片上只有「简介」，点进来才展示完整内容。
 * 垃圾桶（删除）和铅笔（编辑）放在这一层的右上角。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailSheet(
    item: Assignment,
    now: Long,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var viewerIndex by remember(item.id) { mutableStateOf<Int?>(null) }

    val urgency = urgencyOf(item, now)
    val accent = urgency.color
    val progress by animateFloatAsState(
        targetValue = if (item.done) 1f else item.remainingRatioAt(now),
        label = "detailProgress",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(text = urgency.label, color = accent)
                Spacer(Modifier.weight(1f))
                ActionIcon(
                    icon = Icons.Rounded.Edit,
                    label = "编辑这条作业",
                    tint = brandContentColor(),
                    onClick = onEdit,
                )
                Spacer(Modifier.width(8.dp))
                ActionIcon(
                    icon = Icons.Rounded.Delete,
                    label = "删除这条作业",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = item.name.ifBlank { "未命名作业" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.course.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.course,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(18.dp))

            InfoRow(item.timeKind.icon, item.timeKind.fullLabel, formatDateTime(item.reportAt))
            InfoRow(
                icon = Icons.Rounded.Schedule,
                label = "剩余时间",
                value = if (item.done) "已完成" else formatRemaining(item.reportAt, now),
                valueColor = accent,
            )
            InfoRow(
                icon = Icons.Rounded.Today,
                label = "记录于",
                value = formatDateTime(item.createdAt),
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.16f))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(accent)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (item.done) "已完成" else "剩余 ${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }

            if (item.note.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                SectionDivider()
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "备注",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (item.photos.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionDivider()
                Spacer(Modifier.height(14.dp))
                PhotoSection(
                    names = item.photos,
                    onOpen = { index -> viewerIndex = index },
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onToggleDone,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Icon(
                    imageVector = if (item.done) Icons.Rounded.Refresh else Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(if (item.done) "恢复为进行中" else "标记为已完成")
            }
        }
    }

    viewerIndex?.let { index ->
        FullScreenPhotoDialog(
            names = item.photos,
            startIndex = index,
            onDismiss = { viewerIndex = null },
        )
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    )
}
