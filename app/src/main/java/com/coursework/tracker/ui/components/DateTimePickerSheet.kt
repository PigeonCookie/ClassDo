package com.coursework.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coursework.tracker.ui.theme.brandContentColor
import com.coursework.tracker.util.formatDateTime
import com.coursework.tracker.util.toEpochMillis
import com.coursework.tracker.util.toLocalDateTime
import com.coursework.tracker.util.weekdayLabel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

private val ITEM_HEIGHT = 42.dp
private const val VISIBLE_ITEMS = 5

/** 年份可选到哪一年 */
private const val MAX_YEAR = 2099

/**
 * 汇报时间选择器，两种输入方式：
 *  · 滑动：年月日时分五列滚轮，松手自动吸附到最近一格
 *  · 手动：直接敲数字，超范围会自动夹到合法值
 *
 * 这里刻意用「贴底的 Dialog」而不是 ModalBottomSheet：
 * 底部弹层会把没落在滚轮上的竖向手势拿去拖动整个面板，
 * 手指稍微落在滚轮外面（标签行、模式切换那一片）就被面板吃掉，
 * 面板被拖到半开之后滚轮更是完全抢不到手势。Dialog 没有内置拖拽，
 * 竖向手势默认全归滚轮，需要关面板时用「顶部小白条 + 标题」那一块自己拖动。
 */
@Composable
fun DateTimePickerSheet(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val initial = remember(initialMillis) { initialMillis.toLocalDateTime() }
    var year by remember(initialMillis) { mutableIntStateOf(initial.year) }
    var month by remember(initialMillis) { mutableIntStateOf(initial.monthValue) }
    var day by remember(initialMillis) { mutableIntStateOf(initial.dayOfMonth) }
    var hour by remember(initialMillis) { mutableIntStateOf(initial.hour) }
    var minute by remember(initialMillis) { mutableIntStateOf(initial.minute) }
    var manualInput by rememberSaveable { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    // 年份给到 2099，留够远期作业的余地；手动输入的上限也跟着这个走
    val years = remember(today) { (today.year - 1..MAX_YEAR).toList() }
    val months = remember { (1..12).toList() }
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    val daysInMonth = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    val days = remember(daysInMonth) { (1..daysInMonth).toList() }

    // 换月/换年后，如果原来选的是 31 号而新月份只有 30 天，就夹回最后一天
    LaunchedEffect(daysInMonth) {
        if (day > daysInMonth) day = daysInMonth
    }

    fun builtMillis(): Long =
        LocalDateTime.of(year, month, minOf(day, daysInMonth), hour, minute).toEpochMillis()

    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val windowHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

            // 面板往下让开的距离（px）：按住顶部往下拖时跟手，松手后弹回去或滑出去。
            // 拖动过程中用 liveOffset 跟着手指写（每帧一次赋值，不经协程），松手后才交给
            // Animatable 做回弹/滑出，这样才跟得住手指。
            // 初值直接给「整个窗口高度」——第一帧它就在屏幕外，不会先闪一下再滑上来。
            val dragOffset = remember { Animatable(windowHeightPx) }
            var liveOffset by remember { mutableFloatStateOf(0f) }
            var dragging by remember { mutableStateOf(false) }

            // 面板本身的高度（量到之前先拿窗口高度兜底），滑出距离和遮罩淡出都按它算
            var cardHeight by remember { mutableFloatStateOf(0f) }
            val cardSpan = if (cardHeight > 0f) cardHeight else windowHeightPx

            // 当前实际位移：拖动中看手指，其余时候看动画
            val currentOffset = { if (dragging) liveOffset else dragOffset.value }
            val dismissDistance = with(LocalDensity.current) { 88.dp.toPx() }
            val flingVelocity = with(LocalDensity.current) { 800.dp.toPx() }

            val dragState = rememberDraggableState { delta ->
                liveOffset = (liveOffset + delta).coerceAtLeast(0f)
            }

            fun dismissAnimated() {
                scope.launch {
                    dragOffset.animateTo(
                        targetValue = cardSpan,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutLinearInEasing,
                        ),
                    )
                    onDismiss()
                }
            }

            // 进场：从屏幕下沿滑上来
            LaunchedEffect(Unit) {
                dragOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }

            // 点空白处关闭（放在卡片下面当遮罩，卡片是它的兄弟节点，不会被点穿）。
            // 遮罩的深浅直接按位移画：面板往下拖多少，遮罩就淡多少。位移在绘制 lambda 里读，
            // 只重绘不重组，也不会为了透明度单独开一层离屏缓冲（有些设备上那层会画黑）。
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val gone = (currentOffset() / cardSpan).coerceIn(0f, 1f)
                        drawRect(color = Color.Black, alpha = 0.42f * (1f - gone))
                    }
                    .clickable(onClick = { dismissAnimated() })
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .onSizeChanged { cardHeight = it.height.toFloat() }
                    // 位移只在 lambda 里读，拖动时只是重新摆放位置，不触发重组。
                    // 这里用布局位移而不是图层平移：图层平移会让卡片内部的滚轮、模式切换
                    // 收不到触摸（命中测试对不上），布局位移不会。
                    .offset { IntOffset(0, currentOffset().roundToInt()) }
                    // 吃掉落在卡片空白处的点击：不然会"点穿"到底下的遮罩，
                    // 变成关掉面板，看着就像按钮点了没反应
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                ) {
                    // 顶部「小白条 + 标题」这一整块按住就能往下拖：面板跟着手指走，
                    // 松手时拖得够远（或甩得够快）就滑出去关掉，否则弹回原位。
                    // 只有这一块参与拖动，卡片其余部分不抢手势，滚轮才不会被夺走。
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggable(
                                state = dragState,
                                orientation = Orientation.Vertical,
                                onDragStarted = {
                                    // 从动画当前所在的位置接着拖
                                    dragOffset.stop()
                                    liveOffset = dragOffset.value
                                    dragging = true
                                },
                                onDragStopped = { velocity ->
                                    dragging = false
                                    val distance = liveOffset
                                    dragOffset.snapTo(distance)
                                    if (distance > dismissDistance || velocity > flingVelocity) {
                                        dismissAnimated()
                                    } else {
                                        dragOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.85f,
                                                stiffness = Spring.StiffnessMediumLow,
                                            ),
                                        )
                                    }
                                },
                            ),
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.4f
                                            )
                                        )
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "选择时间",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    ModeSwitch(manual = manualInput, onChange = { manualInput = it })

                    Spacer(Modifier.height(14.dp))

                    // 这块拿到剩余高度，滚轮跟着自适应；手动输入时居中
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        if (manualInput) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NumberBox(
                                        value = year,
                                        label = "年",
                                        maxDigits = 4,
                                        range = years.first()..years.last(),
                                        modifier = Modifier.weight(1.5f),
                                        onChange = { year = it },
                                    )
                                    NumberBox(
                                        value = month,
                                        label = "月",
                                        maxDigits = 2,
                                        range = 1..12,
                                        modifier = Modifier.weight(1f),
                                        onChange = { month = it },
                                    )
                                    NumberBox(
                                        value = day,
                                        label = "日",
                                        maxDigits = 2,
                                        range = 1..daysInMonth,
                                        modifier = Modifier.weight(1f),
                                        onChange = { day = it },
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NumberBox(
                                        value = hour,
                                        label = "时",
                                        maxDigits = 2,
                                        range = 0..23,
                                        modifier = Modifier.weight(1f),
                                        onChange = { hour = it },
                                    )
                                    NumberBox(
                                        value = minute,
                                        label = "分",
                                        maxDigits = 2,
                                        range = 0..59,
                                        modifier = Modifier.weight(1f),
                                        onChange = { minute = it },
                                    )
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    WheelLabel("年", Modifier.weight(1.35f))
                                    WheelLabel("月", Modifier.weight(1f))
                                    WheelLabel("日", Modifier.weight(1f))
                                    WheelLabel("时", Modifier.weight(1f))
                                    WheelLabel("分", Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                ) {
                                    // 中间那格的选中条
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(ITEM_HEIGHT)
                                            .align(Alignment.Center)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Wheel(
                                            values = years,
                                            selectedIndex = years.indexOf(year).coerceAtLeast(0),
                                            modifier = Modifier.weight(1.35f).fillMaxHeight(),
                                            onSelected = { year = years[it] },
                                        )
                                        Wheel(
                                            values = months,
                                            selectedIndex = month - 1,
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            onSelected = { month = months[it] },
                                        )
                                        Wheel(
                                            values = days,
                                            selectedIndex = (day - 1).coerceIn(0, days.lastIndex),
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            onSelected = { day = days[it] },
                                        )
                                        Wheel(
                                            values = hours,
                                            selectedIndex = hour,
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            onSelected = { hour = hours[it] },
                                        )
                                        Wheel(
                                            values = minutes,
                                            selectedIndex = minute,
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            labelOf = { String.format("%02d", it) },
                                            onSelected = { minute = minutes[it] },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val preview = builtMillis()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            // 同年不带年份，跨年会自动带上（formatDateTime 里判断）
                            text = "${formatDateTime(preview)} " +
                                weekdayLabel(preview.toLocalDateTime().toLocalDate(), today),
                            style = MaterialTheme.typography.titleSmall,
                            color = brandContentColor(),
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = { onConfirm(builtMillis()) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.7f)
                                .height(50.dp),
                        ) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("确定")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSwitch(manual: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to "滑动选择", true to "手动输入").forEach { (value, label) ->
            val active = value == manual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onChange(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
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
private fun WheelLabel(text: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 一列滚轮。高度跟着父容器自适应（屏幕矮的时候少露几格）。
 *
 * 两个关键点：
 *  1) 用框架自带的吸附惯性，吸附发生在滑动过程里，快速甩动也能稳稳停到整格上；
 *  2) 每跨过一格就立刻把选中值报出去（不等惯性停下），保证预览即时更新、
 *     也保证滑完立刻点确定拿到的是新值；代价靠「明暗在绘制阶段读」压到很低。
 */
@Composable
private fun Wheel(
    values: List<Int>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    labelOf: (Int) -> String = { it.toString() },
    onSelected: (Int) -> Unit,
) {
    if (values.isEmpty()) return

    val density = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { ITEM_HEIGHT.toPx() } }

    BoxWithConstraints(modifier) {
        val available = maxHeight
        // 上下留白各占一半，保证选中格正好落在正中间
        val padding = ((available - ITEM_HEIGHT) / 2).coerceAtLeast(0.dp)

        val state = rememberLazyListState(
            initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, values.lastIndex)
        )
        var lastEmitted by remember { mutableIntStateOf(selectedIndex) }

        val centered by remember(values.size) {
            derivedStateOf {
                (state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset / itemHeightPx)
                    .roundToInt()
                    .coerceIn(0, values.lastIndex)
            }
        }

        // 滚轮 → 外部：每跨过一格就立刻回报。
        // 不能只在「惯性停下来」那一刻回报：那样滑动后要等惯性尾巴走完才更新，
        // 而且滑完立刻点确定会用到旧值（改动丢失）。
        // 这里只做一次轻量状态写入——滚轮项的明暗是绘制阶段读的，不会连带重组，
        // 所以「立刻回报」不会像以前那样卡。
        LaunchedEffect(centered) {
            if (centered != lastEmitted) {
                lastEmitted = centered
                onSelected(centered)
            }
        }

        // 惯性停下来之后补正到整格；框架自带的吸附正常情况下已经对齐，这里是兜底
        LaunchedEffect(state) {
            snapshotFlow { state.isScrollInProgress }
                .filter { !it }
                .collect {
                    val target = centered
                    if (state.firstVisibleItemIndex != target ||
                        state.firstVisibleItemScrollOffset != 0
                    ) {
                        state.animateScrollToItem(target)
                    }
                }
        }

        // 外部 → 滚轮（换月导致天数变化之类）
        LaunchedEffect(selectedIndex) {
            val target = selectedIndex.coerceIn(0, values.lastIndex)
            if (target != lastEmitted) {
                lastEmitted = target
                state.scrollToItem(target)
            }
        }

        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            modifier = Modifier
                .fillMaxWidth()
                .height(available),
            contentPadding = PaddingValues(vertical = padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(values.size) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT)
                        .graphicsLayer {
                            // 在绘制阶段读 centered：只让图层失效，不触发重组
                            val distance = abs(index - centered)
                            alpha = when (distance) {
                                0 -> 1f
                                1 -> 0.45f
                                2 -> 0.22f
                                else -> 0.1f
                            }
                            val scale = if (distance == 0) 1f else 0.86f
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelOf(values[index]),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberBox(
    value: Int,
    label: String,
    maxDigits: Int,
    range: IntRange,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(maxDigits)
            text = digits
            digits.toIntOrNull()?.let { onChange(it.coerceIn(range.first, range.last)) }
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    )
}
