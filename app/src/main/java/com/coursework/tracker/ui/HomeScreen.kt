package com.coursework.tracker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coursework.tracker.MainActivity
import com.coursework.tracker.data.PhotoStore
import com.coursework.tracker.model.Assignment
import com.coursework.tracker.ui.components.AssignmentCard
import com.coursework.tracker.ui.components.AssignmentDetailSheet
import com.coursework.tracker.ui.components.AssignmentEditorSheet
import com.coursework.tracker.ui.components.ColorPickerSheet
import com.coursework.tracker.ui.components.DateStrip
import com.coursework.tracker.ui.components.IconCropDialog
import com.coursework.tracker.ui.components.MoreSheet
import com.coursework.tracker.ui.components.markerColors
import com.coursework.tracker.ui.theme.LocalAppTheme
import com.coursework.tracker.ui.theme.brandContentColor
import com.coursework.tracker.util.formatDate
import com.coursework.tracker.util.toEpochMillisAtStartOfDay
import com.coursework.tracker.util.toLocalDate
import com.coursework.tracker.util.weekdayLabel
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 日期条覆盖的范围：过去的只留到昨天，往后 60 天 */
private const val DAYS_BEFORE = 1
private const val DAYS_AFTER = 60

/** 裁剪图标时原始图最长边的解码上限 */
private const val ICON_SOURCE_MAX_PX = 1200

@Composable
fun HomeScreen(viewModel: AssignmentViewModel, focusId: String? = null) {
    val visibleItems by viewModel.visibleItems.collectAsState()
    val allItems by viewModel.items.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val now by viewModel.now.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dateListState = rememberLazyListState()

    val settings: SettingsViewModel = viewModel()
    val themeMode by settings.themeMode.collectAsState()
    val accent by settings.accent.collectAsState()
    val customIcon by settings.customIcon.collectAsState()

    val scope = rememberCoroutineScope()
    var cropSource by remember { mutableStateOf<Bitmap?>(null) }

    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var moreVisible by rememberSaveable { mutableStateOf(false) }
    var colorPickerVisible by rememberSaveable { mutableStateOf(false) }

    val today = remember(now) { LocalDate.now() }
    val compactHeader = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.screenHeightDp < 600

    val dates = remember(today) {
        generateSequence(today.minusDays(DAYS_BEFORE.toLong())) { it.plusDays(1) }
            .take(DAYS_BEFORE + 1 + DAYS_AFTER)
            .toList()
    }
    val markersByDate = remember(allItems, now) {
        allItems.groupBy { it.reportAt.toLocalDate() }
            .mapValues { (_, list) -> markerColors(list, now) }
    }

    // Android 13+ 需要用户授权才能弹提醒
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // 选图 → 解码 → 进裁剪页
    val iconPickContract = remember { ActivityResultContracts.PickVisualMedia() }
    val iconPicker = rememberLauncherForActivityResult(iconPickContract) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = PhotoStore.decodeFromUri(context, uri, ICON_SOURCE_MAX_PX)
                if (bitmap == null) {
                    snackbarHostState.showSnackbar("这张图读不出来，换一张试试")
                } else {
                    cropSource = bitmap
                }
            }
        }
    }

    LaunchedEffect(focusId, allItems) {
        if (!focusId.isNullOrBlank() && allItems.any { it.id == focusId }) {
            detailId = focusId
        }
    }

    val appTheme = LocalAppTheme.current
    val headerColors = appTheme.accent.gradient(appTheme.isDark)
    val headerFg = appTheme.accent.onHeader(appTheme.isDark)
    val lightHeader = appTheme.accent.isLightHeader(appTheme.isDark)
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            HeaderSection(
                compact = compactHeader,
                stats = stats,
                dates = dates,
                today = today,
                selectedDate = selectedDate,
                markers = { date -> markersByDate[date].orEmpty() },
                dateListState = dateListState,
                colors = headerColors,
                contentColor = headerFg,
                lightHeader = lightHeader,
                onSelectDate = viewModel::selectDate,
                onMore = { moreVisible = true },
                onAdd = {
                    editingId = null
                    editorVisible = true
                },
            )

            Box(Modifier.fillMaxWidth().weight(1f)) {
                // 渐变延续到圆角后面，让内容面板看起来是浮在渐变上的
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(headerColors.last())
                )
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.fillMaxSize()) {
                        FilterRow(
                            filter = filter,
                            count = visibleItems.size,
                            onFilterChange = viewModel::setFilter,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        AssignmentList(
                            items = visibleItems,
                            grouped = selectedDate == null,
                            today = today,
                            now = now,
                            hasAnyItem = allItems.isNotEmpty(),
                            bottomPadding = bottomInset + 28.dp,
                            onOpenDetail = { item -> detailId = item.id },
                            onToggleDone = viewModel::toggleDone,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomInset + 12.dp),
        )
    }

    if (moreVisible) {
        MoreSheet(
            themeMode = themeMode,
            accent = accent,
            customIcon = customIcon,
            onThemeModeChange = settings::setThemeMode,
            onAccentChange = settings::setAccent,
            onPickIcon = {
                // 先收起面板再拉起相册，避免系统选图回来时面板状态错乱
                moreVisible = false
                iconPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onResetIcon = {
                settings.clearCustomIcon()
                scope.launch { snackbarHostState.showSnackbar("已恢复默认图标") }
            },
            onPinIcon = {
                val icon = customIcon
                scope.launch {
                    val message = when {
                        icon == null -> "先上传一张图片"
                        pinIconToHome(context, icon) -> "请在系统弹窗里确认，图标会出现在桌面上"
                        else -> "当前桌面不支持钉图标"
                    }
                    snackbarHostState.showSnackbar(message)
                }
            },
            onDismiss = { moreVisible = false },
            onOpenColorPicker = { colorPickerVisible = true },
        )
    }

    if (colorPickerVisible) {
        ColorPickerSheet(
            initial = accent.seed,
            onDismiss = { colorPickerVisible = false },
            onConfirm = { color ->
                settings.setCustomAccent(color)
                colorPickerVisible = false
                scope.launch { snackbarHostState.showSnackbar("主题色已换成自定义颜色") }
            },
        )
    }

    cropSource?.let { source ->
        IconCropDialog(
            source = source,
            onCancel = { cropSource = null },
            onConfirm = { cropped ->
                settings.setCustomIcon(cropped)
                cropSource = null
                scope.launch { snackbarHostState.showSnackbar("图标已更新") }
            },
        )
    }

    if (editorVisible) {
        AssignmentEditorSheet(
            initial = allItems.firstOrNull { it.id == editingId },
            defaultDate = selectedDate,
            onDeletePhotos = viewModel::deletePhotos,
            onDismiss = {
                editorVisible = false
                editingId = null
            },
            onSave = { item ->
                viewModel.upsert(item)
                editorVisible = false
                editingId = null
            },
        )
    }

    allItems.firstOrNull { it.id == detailId }?.let { target ->
        AssignmentDetailSheet(
            item = target,
            now = now,
            onDismiss = { detailId = null },
            onEdit = {
                detailId = null
                editingId = target.id
                editorVisible = true
            },
            onDelete = {
                detailId = null
                deletingId = target.id
            },
            onToggleDone = { viewModel.toggleDone(target) },
        )
    }

    allItems.firstOrNull { it.id == deletingId }?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("删除这项作业？") },
            text = {
                Text("「${target.name}」、它的提醒和照片都会被移除，删除后无法恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(target)
                        if (detailId == target.id) detailId = null
                        deletingId = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text("取消") }
            },
        )
    }
}

/* ------------------------------------------------------------------ 顶部区域 */

/**
 * 用系统快捷方式把带自定义图标的入口钉到桌面。
 * 安卓不允许第三方应用替换自己的桌面图标（那个由系统读安装包里的资源），
 * 钉快捷方式是唯一能让自定义图片出现在桌面上的正规做法。
 */
private fun pinIconToHome(context: Context, bitmap: Bitmap): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
    val shortcut = ShortcutInfoCompat.Builder(context, "homework_butler_custom_icon")
        .setShortLabel("ClassDo")
        .setIcon(IconCompat.createWithBitmap(bitmap))
        .setIntent(
            Intent(context, MainActivity::class.java).apply { action = Intent.ACTION_MAIN }
        )
        .build()
    return runCatching {
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        true
    }.getOrDefault(false)
}

@Composable
private fun HeaderSection(
    compact: Boolean,
    stats: HomeStats,
    dates: List<LocalDate>,
    today: LocalDate,
    selectedDate: LocalDate?,
    markers: (LocalDate) -> List<Color>,
    dateListState: androidx.compose.foundation.lazy.LazyListState,
    colors: List<Color>,
    contentColor: Color,
    lightHeader: Boolean,
    onSelectDate: (LocalDate?) -> Unit,
    onMore: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 必须用竖直渐变：对角渐变会让底边左右颜色不一致，
            // 和下面圆角衬底最浅的那个颜色对不上，左侧就会出现一条错位的色块
            .background(Brush.verticalGradient(colors))
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "ClassDo",
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor,
                    )
                    if (!compact) {
                        Text(
                            text = "别错过每一次任务",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.78f),
                        )
                    }
                }
                MoreButton(onClick = onMore, contentColor = contentColor)
                Spacer(Modifier.width(10.dp))
                AddButton(onClick = onAdd)
            }

            Spacer(Modifier.height(if (compact) 10.dp else 16.dp))

            if (compact) {
                Text(
                    text = "${stats.active} 项待完成 · ${stats.dueToday} 项今日截止 · " +
                        "${stats.overdue} 项逾期",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.88f),
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        value = stats.active,
                        label = "待完成",
                        contentColor = contentColor,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stats.dueToday,
                        label = "今日截止",
                        contentColor = contentColor,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stats.overdue,
                        label = "已逾期",
                        contentColor = contentColor,
                        modifier = Modifier.weight(1f),
                        alert = stats.overdue > 0,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            DateStrip(
                dates = dates,
                today = today,
                selected = selectedDate,
                markerProvider = markers,
                onSelect = onSelectDate,
                contentColor = contentColor,
                lightHeader = lightHeader,
                listState = dateListState,
                chipHeight = if (compact) 68.dp else 74.dp,
            )
        }
    }
}

@Composable
private fun MoreButton(onClick: () -> Unit, contentColor: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = 0.20f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "更多设置",
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "添加作业",
            tint = brandContentColor(),
            modifier = Modifier.size(25.dp),
        )
    }
}

@Composable
private fun StatCard(
    value: Int,
    label: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    alert: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (alert) Color(0xFFFF6B81).copy(alpha = 0.30f)
                else contentColor.copy(alpha = 0.17f)
            )
            .padding(vertical = 11.dp, horizontal = 12.dp),
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.82f),
        )
    }
}

/* ------------------------------------------------------------------ 列表区域 */

@Composable
private fun FilterRow(
    filter: FilterMode,
    count: Int,
    onFilterChange: (FilterMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = brandContentColor()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(Modifier.padding(4.dp)) {
                FilterMode.values().forEach { mode ->
                    val selected = mode == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                            )
                            .clickable { onFilterChange(mode) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) brand else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count 项",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AssignmentList(
    items: List<Assignment>,
    grouped: Boolean,
    today: LocalDate,
    now: Long,
    hasAnyItem: Boolean,
    bottomPadding: Dp,
    onOpenDetail: (Assignment) -> Unit,
    onToggleDone: (Assignment) -> Unit,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            EmptyState(hasAnyItem = hasAnyItem)
        }
        return
    }

    val groups = if (grouped) items.groupBy { it.reportAt.toLocalDate() } else emptyMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (grouped) {
            groups.forEach { (date, dayItems) ->
                item(key = "header-${date.toEpochDay()}") {
                    DateGroupHeader(date = date, count = dayItems.size, today = today)
                }
                items(items = dayItems, key = { it.id }) { item ->
                    AssignmentCard(
                        item = item,
                        now = now,
                        onClick = { onOpenDetail(item) },
                        onToggleDone = { onToggleDone(item) },
                    )
                }
            }
        } else {
            items(items = items, key = { it.id }) { item ->
                AssignmentCard(
                    item = item,
                    now = now,
                    onClick = { onOpenDetail(item) },
                    onToggleDone = { onToggleDone(item) },
                )
            }
        }
    }
}

@Composable
private fun DateGroupHeader(date: LocalDate, count: Int, today: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 660.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = weekdayLabel(date, today),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatDate(date.toEpochMillisAtStartOfDay()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count 项",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState(hasAnyItem: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (hasAnyItem) Icons.Rounded.EventAvailable else Icons.Rounded.Inbox,
                contentDescription = null,
                tint = brandContentColor(),
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (hasAnyItem) "这里还没有作业" else "还没有记录任何作业",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasAnyItem) {
                "换一天看看，或者点右上角的 + 添加一项"
            } else {
                "点右上角的 + 添加第一项作业，我会在截止时间前提醒你"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
