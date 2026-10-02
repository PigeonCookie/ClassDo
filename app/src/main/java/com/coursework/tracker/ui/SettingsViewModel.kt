package com.coursework.tracker.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coursework.tracker.data.IconStore
import com.coursework.tracker.data.SettingsStore
import com.coursework.tracker.ui.theme.AccentPalette
import com.coursework.tracker.ui.theme.Accents
import com.coursework.tracker.ui.theme.CUSTOM_ACCENT_ID
import com.coursework.tracker.ui.theme.ThemeMode
import com.coursework.tracker.ui.theme.customAccent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val iconStore = IconStore(app)

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(store.themeModeName) }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _accent = MutableStateFlow(
        if (store.accentId == CUSTOM_ACCENT_ID) {
            customAccent(store.customAccentArgb)
        } else {
            Accents.byId(store.accentId)
        }
    )
    val accent: StateFlow<AccentPalette> = _accent.asStateFlow()

    private val _customIcon = MutableStateFlow<Bitmap?>(null)
    val customIcon: StateFlow<Bitmap?> = _customIcon.asStateFlow()

    init {
        viewModelScope.launch { _customIcon.value = iconStore.load() }
    }

    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        store.themeModeName = mode.name
    }

    fun setAccent(palette: AccentPalette) {
        if (palette.id == CUSTOM_ACCENT_ID) return
        if (_accent.value.id == palette.id) return
        _accent.value = palette
        store.accentId = palette.id
    }

    /** 调色盘选色 */
    fun setCustomAccent(color: Color) {
        val argb = color.toArgb()
        store.accentId = CUSTOM_ACCENT_ID
        store.customAccentArgb = argb
        _accent.value = customAccent(argb)
    }

    fun setCustomIcon(bitmap: Bitmap) {
        _customIcon.value = bitmap
        viewModelScope.launch { iconStore.save(bitmap) }
    }

    fun clearCustomIcon() {
        _customIcon.value = null
        viewModelScope.launch { iconStore.clear() }
    }
}
