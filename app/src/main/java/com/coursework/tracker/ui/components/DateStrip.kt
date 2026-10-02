package com.coursework.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coursework.tracker.ui.theme.brandContentColor
import com.coursework.tracker.util.formatMonth
import com.coursework.tracker.util.weekdayLabel
import java.time.LocalDate

/**
 * 顶部横向日期条：可左右滑动，选中某天即筛选当天的作业。
 * 有作业的日期下方会画小圆点，颜色代表紧急程度。
 *
 * [contentColor] / [lightHeader] 由外部按当前主题色算好传进来：
 * 主题色偏亮时头部要用深字，选中态也要反过来（深底浅字），否则整条都看不清。
 */
@Composable
fun DateStrip(
    dates: List<LocalDate>,
    today: LocalDate,
    selected: LocalDate?,
    markerProvider: (LocalDate) -> List<Color>,
    onSelect: (LocalDate?) -> Unit,
    contentColor: Color,
    lightHeader: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    chipHeight: Dp = 74.dp,
) {
    // 首次进入自动滚到今天附近，并把今天左侧留出一点上下文
    LaunchedEffect(today, dates) {
        val index = dates.indexOf(today)
        if (index >= 0) {
            listState.scrollToItem((index + 1 - 2).coerceAtLeast(0))
        }
    }

    val anchorMonth by remember(dates, selected, today) {
        derivedStateOf {
            val firstDateIndex = listState.firstVisibleItemIndex - 1
            dates.getOrNull(firstDateIndex) ?: selected ?: today
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatMonth(anchorMonth),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.86f),
            )
            Spacer(Modifier.weight(1f))
            if (selected != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.20f))
                        .clickable { onSelect(null) }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "看全部",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            item(key = "__all__") {
                AllDatesChip(
                    selected = selected == null,
                    height = chipHeight,
                    contentColor = contentColor,
                    lightHeader = lightHeader,
                    onClick = { onSelect(null) },
                )
            }
            items(items = dates, key = { it.toEpochDay() }) { date ->
                DateChip(
                    date = date,
                    today = today,
                    selected = selected == date,
                    markers = markerProvider(date),
                    height = chipHeight,
                    contentColor = contentColor,
                    lightHeader = lightHeader,
                    onClick = { onSelect(if (selected == date) null else date) },
                )
            }
        }
    }
}

/** 选中态：头部偏亮时反过来用深底浅字，不然白底白字糊在一起 */
@Composable
private fun chipColors(
    selected: Boolean,
    contentColor: Color,
    lightHeader: Boolean,
): Pair<Color, Color> {
    val brand = brandContentColor()
    val surface = MaterialTheme.colorScheme.surface
    return when {
        selected && lightHeader -> contentColor to surface
        selected -> Color.White to brand
        else -> contentColor.copy(alpha = 0.16f) to contentColor
    }
}

@Composable
private fun DateChip(
    date: LocalDate,
    today: LocalDate,
    selected: Boolean,
    markers: List<Color>,
    height: Dp,
    contentColor: Color,
    lightHeader: Boolean,
    onClick: () -> Unit,
) {
    val (background, foreground) = chipColors(selected, contentColor, lightHeader)
    val isToday = date == today

    Column(
        modifier = Modifier
            .width(54.dp)
            .height(height)
            .clip(RoundedCornerShape(19.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = weekdayLabel(date, today),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = foreground.copy(alpha = 0.82f),
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = foreground,
        )
        Spacer(Modifier.height(5.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (markers.isEmpty()) {
                Spacer(Modifier.size(4.dp))
            } else {
                markers.forEach { markerColor ->
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (selected) markerColor else foreground.copy(alpha = 0.9f))
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDatesChip(
    selected: Boolean,
    height: Dp,
    contentColor: Color,
    lightHeader: Boolean,
    onClick: () -> Unit,
) {
    val (background, foreground) = chipColors(selected, contentColor, lightHeader)

    Column(
        modifier = Modifier
            .width(58.dp)
            .height(height)
            .clip(RoundedCornerShape(19.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "全部",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
    }
}
