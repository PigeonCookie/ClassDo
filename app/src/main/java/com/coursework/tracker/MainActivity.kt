package com.coursework.tracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coursework.tracker.ui.AssignmentViewModel
import com.coursework.tracker.ui.HomeScreen
import com.coursework.tracker.ui.SettingsViewModel
import com.coursework.tracker.ui.theme.ClassDoTheme
import com.coursework.tracker.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    /** 从通知点进来时，希望自动展开的那条作业 */
    private val focusIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 全屏沉浸式：内容延伸到状态栏/导航栏下方，由 Compose 用 WindowInsets 自己留白
        enableEdgeToEdge()

        focusIdState.value = intent?.getStringExtra(EXTRA_FOCUS_ID)

        setContent {
            val settings: SettingsViewModel = viewModel()
            val themeMode by settings.themeMode.collectAsState()
            val accent by settings.accent.collectAsState()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ClassDoTheme(darkTheme = darkTheme, accent = accent) {
                val viewModel: AssignmentViewModel = viewModel()
                HomeScreen(viewModel = viewModel, focusId = focusIdState.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        focusIdState.value = intent.getStringExtra(EXTRA_FOCUS_ID)
    }

    companion object {
        const val EXTRA_FOCUS_ID = "extra_focus_id"
    }
}
