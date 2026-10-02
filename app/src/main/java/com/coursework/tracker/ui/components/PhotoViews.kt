package com.coursework.tracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.coursework.tracker.data.PhotoStore
import com.coursework.tracker.ui.theme.brandContentColor
import kotlin.math.roundToInt

/** 异步把本地照片解码成 ImageBitmap，解码过程带降采样和内存缓存 */
@Composable
fun rememberPhotoBitmap(name: String, maxSizePx: Int): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name, maxSizePx) {
        value = PhotoStore.load(context, name, maxSizePx)?.asImageBitmap()
    }
    return bitmap
}

@Composable
fun PhotoThumb(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val maxPx = remember(size, density) {
        with(density) { (size * 2.5f).roundToPx() }.coerceAtLeast(256)
    }
    val bitmap = rememberPhotoBitmap(name, maxPx)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }

        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(21.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "移除这张照片",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** 横向排列的照片条，末尾可带一个「添加照片」方块 */
@Composable
fun PhotoStrip(
    names: List<String>,
    thumbSize: Dp = 92.dp,
    onOpen: (Int) -> Unit,
    onRemove: ((String) -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding,
    ) {
        items(items = names, key = { it }) { name ->
            val index = names.indexOf(name)
            PhotoThumb(
                name = name,
                size = thumbSize,
                onClick = { onOpen(index) },
                onRemove = onRemove?.let { remove -> { remove(name) } },
            )
        }
        if (onAdd != null) {
            item(key = "__add__") {
                AddPhotoTile(size = thumbSize, onClick = onAdd)
            }
        }
    }
}

@Composable
fun AddPhotoTile(size: Dp, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.AddPhotoAlternate,
            contentDescription = "从相册添加照片",
            tint = brandContentColor(),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "添加照片",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 全屏看图：可以双指放大、双击缩放、放大后拖动；点空白处关闭，多张时可左右翻 */
@Composable
fun FullScreenPhotoDialog(
    names: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    if (names.isEmpty()) return
    var index by remember(names) {
        mutableIntStateOf(startIndex.coerceIn(0, names.lastIndex))
    }
    val current = names.getOrNull(index) ?: return
    val bitmap = rememberPhotoBitmap(current, PhotoStore.FULL)

    // 缩放状态：换到别的照片就重置
    var scale by remember(current) { mutableStateOf(1f) }
    var offset by remember(current) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(candidate: Offset, at: Float): Offset {
        val maxX = (viewport.width * (at - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (viewport.height * (at - 1f) / 2f).coerceAtLeast(0f)
        return Offset(
            candidate.x.coerceIn(-maxX, maxX),
            candidate.y.coerceIn(-maxY, maxY),
        )
    }

    fun resetZoom() {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .statusBarsPadding()
                .onSizeChanged { viewport = it }
                // 双指缩放 + 放大后拖动
                .pointerInput(current) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(1f, 6f)
                        scale = next
                        offset = clampOffset(offset + pan, next)
                    }
                }
                // 双击缩放；点一下——放大状态先复位，没放大才关闭
                .pointerInput(current) {
                    detectTapGestures(
                        onTap = { if (scale > 1f) resetZoom() else onDismiss() },
                        onDoubleTap = { if (scale > 1f) resetZoom() else scale = 2.5f },
                    )
                },
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
            }

            if (scale <= 1f) {
                Text(
                    text = if (names.size > 1) "双指放大 · 双击缩放 · 左右翻页" else "双指放大 · 双击缩放",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1} / ${names.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (names.size > 1) {
                PagerButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    label = "上一张",
                    enabled = index > 0,
                    modifier = Modifier.align(Alignment.CenterStart).padding(8.dp),
                    onClick = { index -= 1 },
                )
                PagerButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    label = "下一张",
                    enabled = index < names.lastIndex,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp),
                    onClick = { index += 1 },
                )
            }
        }
    }
}

@Composable
private fun PagerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.16f else 0.05f))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.3f),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 详情页里那块「照片 (3)」区域 */
@Composable
fun PhotoSection(
    names: List<String>,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "照片",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${names.size} 张",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        PhotoStrip(
            names = names,
            thumbSize = 96.dp,
            onOpen = onOpen,
        )
    }
}
