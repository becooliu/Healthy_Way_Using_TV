package com.healthyway.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthyway.tv.ui.main.MainScreen
import com.healthyway.tv.ui.main.MainViewModel
import com.healthyway.tv.ui.settings.SettingsViewModel
import com.healthyway.tv.ui.theme.HealthyWayTheme

/**
 * 主 Activity
 * 【功能】应用入口，承载主界面和设置界面
 * 【说明】使用 Compose 单 Activity 架构，通过 ViewModel 控制界面切换
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HealthyWayTheme {
                val mainViewModel: MainViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()

                MainScreen(
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
