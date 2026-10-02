package com.coursework.tracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** 显示模式 */
enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

/** 相对亮度，所有「该配白字还是深字」的判断都基于它 */
fun Color.relativeLuminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

private val DarkOnLight = Color(0xFF17191F)

/** 这个底色上该配白字还是深字 */
fun Color.readableOn(): Color = if (relativeLuminance() > 0.55f) DarkOnLight else Color.White

/**
 * 当文字或图标压在浅色底上时用的品牌色。
 * 用户挑的原色本身不做任何加工（填充、顶部渐变都是原色），
 * 但「白底上的一行小字」如果也用原色，挑到黄、青、亮绿时就完全看不见了，
 * 所以这个角色会压到看得清为止。内置预设都在 0.47 以下，原样通过。
 */
private const val MAX_TEXT_LUMINANCE = 0.52f

fun Color.readableAsText(): Color {
    val luminance = relativeLuminance()
    if (luminance <= MAX_TEXT_LUMINANCE) return this
    val amount = ((luminance - MAX_TEXT_LUMINANCE) / luminance).coerceIn(0f, 0.82f)
    return lerp(this, Color.Black, amount)
}

/**
 * 一套主题色。只需要给定一个主色种子，容器色、暗色变体、头部渐变都由它算出来，
 * 所以以后想加新颜色，在 [Accents.all] 里加一行就行。
 */
data class AccentPalette(
    val id: String,
    val label: String,
    val seed: Color,
) {
    /** 顶部渐变：浅色模式由主色往白里提，暗色模式往黑里压 */
    fun gradient(isDark: Boolean): List<Color> = if (isDark) {
        listOf(lerp(seed, Color.Black, 0.54f), lerp(seed, Color.Black, 0.30f))
    } else {
        listOf(seed, lerp(seed, Color.White, 0.26f))
    }

    /** 顶部渐变上该用什么颜色的字：种子太亮就翻成深字，否则白字 */
    fun onHeader(isDark: Boolean): Color = gradient(isDark).last().readableOn()

    /** 头部是不是偏亮（选中态要反过来用深底浅字） */
    fun isLightHeader(isDark: Boolean): Boolean =
        gradient(isDark).last().relativeLuminance() > 0.55f

    /** 浅底上当文字/图标用的品牌色 */
    fun brandOnSurface(isDark: Boolean): Color =
        if (isDark) lerp(seed, Color.White, 0.55f) else seed.readableAsText()

    // ---- 浅色模式：填充保持原色，配字自动翻深浅 ----
    val lightPrimary: Color get() = seed
    val lightOnPrimary: Color get() = seed.readableOn()
    val lightPrimaryContainer: Color get() = lerp(seed, Color.White, 0.86f)
    val lightOnPrimaryContainer: Color get() = seed.readableAsText()

    val darkPrimary: Color get() = lerp(seed, Color.White, 0.44f)
    val darkOnPrimary: Color get() = lerp(seed, Color.Black, 0.74f)
    val darkPrimaryContainer: Color get() = lerp(seed, Color.Black, 0.52f)
    val darkOnPrimaryContainer: Color get() = lerp(seed, Color.White, 0.84f)
}

object Accents {

    val Indigo = AccentPalette("indigo", "靛蓝", Color(0xFF4F46E5))
    val Violet = AccentPalette("violet", "紫罗兰", Color(0xFF7C3AED))
    val Sky = AccentPalette("sky", "天蓝", Color(0xFF0284C7))
    val Teal = AccentPalette("teal", "青绿", Color(0xFF0D9488))
    val Graphite = AccentPalette("graphite", "石墨", Color(0xFF475569))

    val all: List<AccentPalette> = listOf(Indigo, Violet, Sky, Teal, Graphite)

    val Default: AccentPalette = Indigo

    fun byId(id: String?): AccentPalette =
        all.firstOrNull { it.id == id } ?: Default
}

/** 用户用调色盘自己挑的那个颜色 */
const val CUSTOM_ACCENT_ID = "custom"

/** 挑到什么样就用什么样，不做任何明暗加工 */
fun customAccent(argb: Int): AccentPalette =
    AccentPalette(CUSTOM_ACCENT_ID, "自定义", Color(argb))

/** 当前生效的外观，供需要拿主色/渐变色的地方读取 */
data class AppTheme(
    val accent: AccentPalette = Accents.Default,
    val isDark: Boolean = false,
)

val LocalAppTheme = staticCompositionLocalOf { AppTheme() }

/** 浅底上当文字/图标用的品牌色 */
@Composable
fun brandContentColor(): Color {
    val theme = LocalAppTheme.current
    return theme.accent.brandOnSurface(theme.isDark)
}

/** 顶部渐变上该用的文字颜色 */
@Composable
fun headerContentColor(): Color {
    val theme = LocalAppTheme.current
    return theme.accent.onHeader(theme.isDark)
}
