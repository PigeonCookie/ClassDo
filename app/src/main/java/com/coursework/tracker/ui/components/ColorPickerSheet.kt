package com.coursework.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.graphics.Color as AndroidColor

private val RAINBOW = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
)

/**
 * 调色盘：上面一块「饱和度 × 明度」方块，下面一条色相条，都能点也能拖。
 * 挑出来的颜色会当主题色的种子，容器色、暗色变体、顶部渐变都由它推导。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val startHsv = remember(initial) {
        val argb = initial.toArgb()
        FloatArray(3).also { hsv ->
            AndroidColor.RGBToHSV(
                (argb shr 16) and 0xFF,
                (argb shr 8) and 0xFF,
                argb and 0xFF,
                hsv,
            )
        }
    }
    var hue by remember(initial) { mutableFloatStateOf(startHsv[0]) }
    var saturation by remember(initial) { mutableFloatStateOf(startHsv[1]) }
    var brightness by remember(initial) { mutableFloatStateOf(startHsv[2]) }

    var squareSize by remember { mutableStateOf(IntSize.Zero) }
    var hueWidth by remember { mutableStateOf(0) }

    val picked = Color.hsv(hue, saturation, brightness)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Text(
                text = "自定义主题色",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "拖动选颜色，方块管浓淡和明暗，色相条管色调",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // 饱和度 × 明度
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.hsv(hue, 1f, 1f))
                    .onSizeChanged { squareSize = it }
                    .pointerInput(Unit) {
                        detectTapGestures { position ->
                            val w = squareSize.width.toFloat()
                            val h = squareSize.height.toFloat()
                            if (w > 0f && h > 0f) {
                                saturation = (position.x / w).coerceIn(0f, 1f)
                                brightness = 1f - (position.y / h).coerceIn(0f, 1f)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val w = squareSize.width.toFloat()
                            val h = squareSize.height.toFloat()
                            if (w > 0f && h > 0f) {
                                saturation = (change.position.x / w).coerceIn(0f, 1f)
                                brightness = 1f - (change.position.y / h).coerceIn(0f, 1f)
                            }
                        }
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Color.White, Color.Transparent))
                        )
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                        )
                )
                Canvas(Modifier.fillMaxSize()) {
                    val cx = saturation * size.width
                    val cy = (1f - brightness) * size.height
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = 12.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 色相
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(RAINBOW))
                    .onSizeChanged { hueWidth = it.width }
                    .pointerInput(Unit) {
                        detectTapGestures { position ->
                            if (hueWidth > 0) {
                                hue = (position.x / hueWidth).coerceIn(0f, 1f) * 360f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            if (hueWidth > 0) {
                                hue = (change.position.x / hueWidth).coerceIn(0f, 1f) * 360f
                            }
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = (hue / 360f) * size.width
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = size.height / 2f - 1.dp.toPx(),
                        center = Offset(cx, size.height / 2f),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                    drawCircle(
                        color = Color.White,
                        radius = size.height / 2f - 3.dp.toPx(),
                        center = Offset(cx, size.height / 2f),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(picked)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "自定义",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = String.format("#%06X", 0xFFFFFF and picked.toArgb()),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(18.dp))

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
                    onClick = { onConfirm(picked) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.7f)
                        .height(50.dp),
                ) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("用这个颜色")
                }
            }
        }
    }
}
