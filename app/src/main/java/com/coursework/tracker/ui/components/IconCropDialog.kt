package com.coursework.tracker.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.roundToInt

/** 自定义图标输出尺寸，正方形 */
private const val ICON_OUTPUT_SIZE = 512

/**
 * 把选中的图片裁成 1:1 的方形图标。
 * 取景框固定为正方形（和图标比例一致），图片按「铺满」起步，可以拖动、双指缩放。
 */
@Composable
fun IconCropDialog(
    source: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    val density = LocalDensity.current
    val viewportSize = 272.dp
    val viewportPx = remember(density, viewportSize) { with(density) { viewportSize.toPx() } }

    val imageBitmap = remember(source) { source.asImageBitmap() }

    // 让图片刚好铺满方形取景框的基础缩放
    val baseScale = remember(source, viewportPx) {
        max(viewportPx / source.width, viewportPx / source.height)
    }

    var userScale by remember(source) { mutableStateOf(1f) }
    var offset by remember(source) { mutableStateOf(Offset.Zero) }

    val scale = baseScale * userScale
    val displayWidth = source.width * scale
    val displayHeight = source.height * scale
    val maxOffsetX = ((displayWidth - viewportPx) / 2f).coerceAtLeast(0f)
    val maxOffsetY = ((displayHeight - viewportPx) / 2f).coerceAtLeast(0f)

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "取消",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "裁剪图标",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            userScale = 1f
                            offset = Offset.Zero
                        },
                    ) {
                        Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重置")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(viewportSize)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.Black)
                            .pointerInput(source, baseScale) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val nextScale = (userScale * zoom).coerceIn(1f, 6f)
                                    val nextS = baseScale * nextScale
                                    val limitX =
                                        ((source.width * nextS - viewportPx) / 2f).coerceAtLeast(0f)
                                    val limitY =
                                        ((source.height * nextS - viewportPx) / 2f).coerceAtLeast(0f)
                                    userScale = nextScale
                                    offset = Offset(
                                        (offset.x + pan.x).coerceIn(-limitX, limitX),
                                        (offset.y + pan.y).coerceIn(-limitY, limitY),
                                    )
                                }
                            },
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val left = viewportPx / 2f - displayWidth / 2f + offset.x
                            val top = viewportPx / 2f - displayHeight / 2f + offset.y
                            drawImage(
                                image = imageBitmap,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(source.width, source.height),
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = IntSize(
                                    displayWidth.roundToInt().coerceAtLeast(1),
                                    displayHeight.roundToInt().coerceAtLeast(1),
                                ),
                                filterQuality = FilterQuality.Medium,
                            )

                            // 三分线，方便对齐
                            val guide = Color.White.copy(alpha = 0.22f)
                            val stroke = 1.dp.toPx()
                            for (i in 1..2) {
                                val p = viewportPx * i / 3f
                                drawLine(guide, Offset(p, 0f), Offset(p, viewportPx), stroke)
                                drawLine(guide, Offset(0f, p), Offset(viewportPx, p), stroke)
                            }
                        }
                    }
                }

                Text(
                    text = "拖动调整位置，双指缩放。取景框是正方形，和图标比例一致；" +
                        "部分桌面会把图标裁成圆形，重要内容尽量放中间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 18.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onConfirm(cropSquare(source, scale, offset, viewportPx))
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.7f)
                            .height(50.dp),
                    ) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("用作图标")
                    }
                }
            }
        }
    }
}

/** 从当前变换里算出取景框对应的原图区域，再缩放到 512×512 */
private fun cropSquare(
    source: Bitmap,
    scale: Float,
    offset: Offset,
    viewportPx: Float,
): Bitmap {
    val displayWidth = source.width * scale
    val displayHeight = source.height * scale
    val left = viewportPx / 2f - displayWidth / 2f + offset.x
    val top = viewportPx / 2f - displayHeight / 2f + offset.y

    val rawSide = (viewportPx / scale).roundToInt().coerceAtLeast(1)
    var side = minOf(rawSide, source.width, source.height)
    val x = (-left / scale).roundToInt().coerceIn(0, (source.width - side).coerceAtLeast(0))
    val y = (-top / scale).roundToInt().coerceIn(0, (source.height - side).coerceAtLeast(0))
    side = minOf(side, source.width - x, source.height - y).coerceAtLeast(1)

    val cropped = Bitmap.createBitmap(source, x, y, side, side)
    val scaled = Bitmap.createScaledBitmap(cropped, ICON_OUTPUT_SIZE, ICON_OUTPUT_SIZE, true)
    if (scaled !== cropped && cropped !== source) cropped.recycle()
    return scaled
}
