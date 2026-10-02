package com.coursework.tracker.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coursework.tracker.R
import com.coursework.tracker.ui.theme.AccentPalette
import com.coursework.tracker.ui.theme.Accents
import com.coursework.tracker.ui.theme.CUSTOM_ACCENT_ID
import com.coursework.tracker.ui.theme.ThemeMode
import com.coursework.tracker.ui.theme.brandContentColor

/**
 * 「更多」面板。目前只有外观设置，以后新的功能按 [MoreSection] 的形式往下加就行。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(
    themeMode: ThemeMode,
    accent: AccentPalette,
    customIcon: Bitmap?,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (AccentPalette) -> Unit,
    onPickIcon: () -> Unit,
    onResetIcon: () -> Unit,
    onPinIcon: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Text(
                text = "更多",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "外观可以按自己的喜好调整",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MoreSection(title = "主题色", trailing = accent.label) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    items(Accents.all, key = { it.id }) { palette ->
                        AccentSwatch(
                            palette = palette,
                            selected = palette.id == accent.id,
                            onClick = { onAccentChange(palette) },
                        )
                    }
                    item(key = "__custom__") {
                        CustomAccentSwatch(
                            selected = accent.id == CUSTOM_ACCENT_ID,
                            customSeed = if (accent.id == CUSTOM_ACCENT_ID) accent.seed else null,
                            onClick = onOpenColorPicker,
                        )
                    }
                }
            }

            MoreSection(title = "显示模式") {
                SegmentedRow(
                    options = ThemeMode.values().toList(),
                    selected = themeMode,
                    labelOf = { it.label },
                    onSelect = onThemeModeChange,
                )
            }

            MoreSection(title = "应用图标") {
                IconPickerBlock(
                    customIcon = customIcon,
                    onPickIcon = onPickIcon,
                    onResetIcon = onResetIcon,
                    onPinIcon = onPinIcon,
                )
            }

            Spacer(Modifier.height(24.dp))

            // 底部版权声明
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "© 2026 咕子曲奇GuZzz",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "开发过程由 DeepSeek 辅助",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun IconPickerBlock(
    customIcon: Bitmap?,
    onPickIcon: () -> Unit,
    onResetIcon: () -> Unit,
    onPinIcon: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconPreview(customIcon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (customIcon == null) "当前是默认图标" else "已使用自定义图标",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "自定义图标将生成桌面快捷方式，请点击下方按钮添加。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onPickIcon,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("上传图片")
        }
        OutlinedButton(
            onClick = onResetIcon,
            enabled = customIcon != null,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text("恢复默认")
        }
    }

    Spacer(Modifier.height(10.dp))

    OutlinedButton(
        onClick = onPinIcon,
        enabled = customIcon != null,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.AddToHomeScreen, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("把图标钉到桌面")
    }
}

@Composable
private fun IconPreview(bitmap: Bitmap?) {
    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(RoundedCornerShape(17.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // 按桌面遮罩的比例放大 1.5 倍再裁切，预览才和桌面看到的一致
            Box(Modifier.size(99.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun MoreSection(
    title: String,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = brandContentColor(),
                )
            }
        }
        content()
    }
}

@Composable
private fun AccentSwatch(
    palette: AccentPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(palette.seed),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** 最后一个：调色盘。没选过时是彩色环，选过就显示挑的那个颜色。 */
@Composable
private fun CustomAccentSwatch(
    selected: Boolean,
    customSeed: Color?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (customSeed != null) {
                        Modifier.background(customSeed)
                    } else {
                        Modifier.background(Brush.sweepGradient(RAINBOW_SWEEP))
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                selected -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                customSeed == null -> Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

private val RAINBOW_SWEEP = listOf(
    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000),
)

@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelOf(option),
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
