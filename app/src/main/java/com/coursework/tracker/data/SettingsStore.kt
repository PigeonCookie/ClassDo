package com.coursework.tracker.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 外观等偏好设置，用 SharedPreferences 存，量很小。
 *
 * 这里一律用 commit() 同步落盘：这些写入都是用户点一下才发生一次，不差这点时间，
 * 但 apply() 是异步刷盘，紧跟着安装/强杀进程时可能丢掉。
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("homework_butler_settings", Context.MODE_PRIVATE)

    var themeModeName: String
        get() = prefs.getString(KEY_THEME_MODE, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_THEME_MODE, value) }
        }

    var accentId: String
        get() = prefs.getString(KEY_ACCENT, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_ACCENT, value) }
        }

    /** 调色盘挑出来的颜色，accentId 为 custom 时生效 */
    var customAccentArgb: Int
        get() = prefs.getInt(KEY_CUSTOM_ACCENT, DEFAULT_CUSTOM_ACCENT)
        set(value) {
            prefs.edit { putInt(KEY_CUSTOM_ACCENT, value) }
        }

    private inline fun SharedPreferences.edit(block: SharedPreferences.Editor.() -> Unit) {
        val editor = edit()
        editor.block()
        editor.commit()
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT = "accent_id"
        const val KEY_CUSTOM_ACCENT = "custom_accent_argb"

        /** 兜底用默认的靛蓝 */
        const val DEFAULT_CUSTOM_ACCENT = 0xFF4F46E5.toInt()
    }
}
