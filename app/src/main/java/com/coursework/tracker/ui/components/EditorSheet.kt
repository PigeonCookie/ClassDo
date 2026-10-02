package com.coursework.tracker.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coursework.tracker.data.DraftStore
import com.coursework.tracker.data.PhotoStore
import com.coursework.tracker.model.Assignment
import com.coursework.tracker.model.TimeKind
import com.coursework.tracker.ui.theme.StatusToday
import com.coursework.tracker.ui.theme.brandContentColor
import com.coursework.tracker.util.formatClock
import com.coursework.tracker.util.toEpochMillis
import com.coursework.tracker.util.toLocalDate
import com.coursework.tracker.util.toLocalDateTime
import com.coursework.tracker.util.weekdayLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

private const val MAX_PHOTOS = 9

/**
 * 新建 / 编辑作业的表单。initial 为 null 表示新建。
 *
 * @param onDeletePhotos 放弃或移除照片时，把待删除的文件名交给外部（ViewModel）去删，
 *                       这样协程不会随表单关闭而被取消。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentEditorSheet(
    initial: Assignment?,
    defaultDate: LocalDate?,
    onDeletePhotos: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Assignment) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 新建和编辑都会先看一眼有没有还没过期的草稿，有就把上次填的内容带回来
    val draft = remember { DraftStore.load(context, initial?.id) }
    val isNew = initial == null
    val restoredDraft = draft != null

    val initialPhotos = remember(initial?.id) { draft?.photos ?: initial?.photos.orEmpty() }
    var photos by remember(initial?.id) { mutableStateOf(initialPhotos) }
    var removedPhotos by remember(initial?.id) { mutableStateOf<List<String>>(emptyList()) }
    var viewerIndex by remember(initial?.id) { mutableStateOf<Int?>(null) }

    val pickContract = remember { ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS) }
    val photoPicker = rememberLauncherForActivityResult(pickContract) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val added = uris.mapNotNull { uri -> PhotoStore.import(context, uri) }
                if (added.isNotEmpty()) {
                    photos = photos + added
                }
            }
        }
    }

    var name by remember { mutableStateOf(draft?.name ?: initial?.name.orEmpty()) }
    var course by remember { mutableStateOf(draft?.course ?: initial?.course.orEmpty()) }
    var timeKind by remember {
        mutableStateOf(draft?.timeKind ?: initial?.timeKind ?: TimeKind.DEADLINE)
    }
    var reportAt by remember {
        mutableLongStateOf(draft?.reportAt ?: initial?.reportAt ?: defaultReportAt(defaultDate))
    }
    var note by remember { mutableStateOf(draft?.note ?: initial?.note.orEmpty()) }
    var showPicker by remember { mutableStateOf(false) }

    // 改动停下来 400ms 就自动写一份草稿（新建、编辑都存）。
    // 编辑模式下只有真的改过才存——否则每次点进来都弹「已恢复」会很烦。
    LaunchedEffect(name, course, timeKind, reportAt, note, photos) {
        val trimmedName = name.trim()
        val trimmedCourse = course.trim()
        val trimmedNote = note.trim()
        val hasContent = trimmedName.isNotBlank() || trimmedCourse.isNotBlank() ||
            trimmedNote.isNotBlank() || photos.isNotEmpty()
        val changed = initial == null || initial.let {
            it.name != trimmedName || it.course != trimmedCourse || it.timeKind != timeKind ||
                it.reportAt != reportAt || it.note != trimmedNote || it.photos != photos
        }

        if (!hasContent || !changed) {
            DraftStore.consume(context)
            return@LaunchedEffect
        }

        delay(400)
        val template = draft ?: initial
        DraftStore.save(
            context,
            initial?.id,
            Assignment(
                id = template?.id ?: UUID.randomUUID().toString(),
                name = trimmedName,
                course = trimmedCourse,
                timeKind = timeKind,
                reportAt = reportAt,
                createdAt = template?.createdAt ?: System.currentTimeMillis(),
                note = trimmedNote,
                photos = photos,
            ),
        )
    }

    val reportDateTime = reportAt.toLocalDateTime()
    val reportDate = reportDateTime.toLocalDate()
    val inPast = reportAt < System.currentTimeMillis()

    fun build(): Assignment = (initial ?: Assignment()).copy(
        name = name.trim(),
        course = course.trim(),
        timeKind = timeKind,
        reportAt = reportAt,
        note = note.trim(),
        photos = photos,
    )

    // 关掉不当成"放弃"：内容已经进草稿了，5 分钟内回来还在，照片也留着
    // （草稿过期时 DraftStore.discard 会把它们一起清掉）
    fun closeSheet() {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { closeSheet() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = if (initial == null) "新建作业" else "编辑作业",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (restoredDraft) {
                    "已恢复之前保存的内容，5分钟后自动删除"
                } else {
                    "填好名称和截止时间，将任务添加到列表中"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("作业名称") },
                placeholder = { Text("例如：数据结构实验报告") },
                leadingIcon = { Icon(Icons.Rounded.Book, null, Modifier.size(20.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text("课程名称（可选）") },
                placeholder = { Text("例如：计算机组成原理") },
                leadingIcon = { Icon(Icons.Rounded.School, null, Modifier.size(20.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("截止/汇报时间")

            TimeKindSelector(selected = timeKind, onSelect = { timeKind = it })

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(
                    icon = Icons.Rounded.CalendarMonth,
                    caption = "日期",
                    value = "${reportDate.monthValue} 月 ${reportDate.dayOfMonth} 日 · " +
                        weekdayLabel(reportDate, LocalDate.now()),
                    modifier = Modifier.weight(1f),
                    onClick = { showPicker = true },
                )
                PickerField(
                    icon = Icons.Rounded.Schedule,
                    caption = "时间",
                    value = formatClock(reportDateTime),
                    modifier = Modifier.weight(0.82f),
                    onClick = { showPicker = true },
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "点日期或时间都能修改：可以上下滑动选，也可以切到手动输入直接敲。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (inPast) {
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = StatusToday,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "这个时间已经过去，保存后会直接显示为逾期",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusToday,
                    )
                }
            }

            SectionLabel("照片（可选）")

            PhotoStrip(
                names = photos,
                thumbSize = 88.dp,
                onOpen = { index -> viewerIndex = index },
                onRemove = { photo ->
                    photos = photos - photo
                    if (isNew) {
                        // 新建时明确删掉的照片立刻清文件，免得草稿过期后变成孤儿
                        onDeletePhotos(listOf(photo))
                    } else if (photo in initialPhotos) {
                        // 编辑已有作业：先记下来，等真的保存时才删（关掉的话原作业还要用）
                        removedPhotos = removedPhotos + photo
                    } else {
                        // 编辑时新加进来又删掉的，直接清掉
                        onDeletePhotos(listOf(photo))
                    }
                },
                onAdd = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )

            if (photos.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "从手机相册里选，最多 $MAX_PHOTOS 张；图片会复制到应用里，相册里删掉也不影响。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "共 ${photos.size} 张，点图片可放大查看",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel("备注（可选）")

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("要交到哪里、需要带什么、注意事项…") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { closeSheet() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Text("关闭")
                }
                Button(
                    onClick = {
                        if (removedPhotos.isNotEmpty()) onDeletePhotos(removedPhotos)
                        // 存成作业了，草稿清掉；照片归这条作业，不能跟着删
                        DraftStore.consume(context)
                        onSave(build())
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.7f).height(50.dp),
                ) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (initial == null) "添加作业" else "保存修改")
                }
            }
        }
    }

    viewerIndex?.let { index ->
        FullScreenPhotoDialog(
            names = photos,
            startIndex = index,
            onDismiss = { viewerIndex = null },
        )
    }

    if (showPicker) {
        DateTimePickerSheet(
            initialMillis = reportAt,
            onDismiss = { showPicker = false },
            onConfirm = { millis ->
                reportAt = millis
                showPicker = false
            },
        )
    }
}

private fun defaultReportAt(date: LocalDate?): Long =
    (date ?: LocalDate.now().plusDays(1)).atTime(0, 0).toEpochMillis()

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 18.dp, bottom = 9.dp),
    )
}

@Composable
private fun TimeKindSelector(selected: TimeKind, onSelect: (TimeKind) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TimeKind.values().forEach { kind ->
            val active = kind == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onSelect(kind) }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = kind.icon,
                    contentDescription = null,
                    tint = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = kind.fullLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun PickerField(
    icon: ImageVector,
    caption: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = brandContentColor(),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
