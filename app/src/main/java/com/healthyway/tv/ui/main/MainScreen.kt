package com.healthyway.tv.ui.main

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthyway.tv.data.TimerState
import com.healthyway.tv.ui.settings.SettingsScreen
import com.healthyway.tv.ui.settings.SettingsViewModel

/**
 * 主界面 Composable
 * 【功能】应用首页，显示计时状态，提供开始/停止和进入设置的操作入口
 * 【说明】根据计时状态显示不同内容
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by mainViewModel.uiState.collectAsState()

    // 如果显示设置界面
    if (uiState.showSettings) {
        SettingsScreen(
            viewModel = settingsViewModel,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 应用标题
        Text(
            text = "健康看电视",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "合理控制观看时间，关爱身心健康",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 状态显示区域
        StatusSection(
            state = uiState.timerState,
            remaining = mainViewModel.getFormattedRemaining(),
            totalMinutes = uiState.config.durationMinutes
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 操作按钮
        when (uiState.timerState) {
            TimerState.IDLE -> {
                // 开始计时按钮
                Button(
                    onClick = mainViewModel::startTimer,
                    modifier = Modifier
                        .width(300.dp)
                        .height(64.dp)
                        .focusable(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "开始计时",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            TimerState.RUNNING, TimerState.WARNING -> {
                // 停止计时按钮
                Button(
                    onClick = mainViewModel::stopTimer,
                    modifier = Modifier
                        .width(300.dp)
                        .height(64.dp)
                        .focusable(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "停止计时",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            TimerState.LOCKED -> {
                Text(
                    text = "屏幕已锁定，请输入密码解锁",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 设置按钮
        OutlinedButton(
            onClick = {
                mainViewModel.refreshConfig()
                mainViewModel.toggleSettings()
            },
            modifier = Modifier
                .width(300.dp)
                .height(56.dp)
                .focusable()
        ) {
            Text(
                text = "使用设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 状态显示区域组件
 * @param state 当前计时状态
 * @param remaining 剩余时间文本
 * @param totalMinutes 总时长（分钟）
 */
@Composable
private fun StatusSection(
    state: TimerState,
    remaining: String,
    totalMinutes: Int
) {
    Card(
        modifier = Modifier
            .width(420.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态标签
            val (statusText, statusColor) = when (state) {
                TimerState.IDLE -> "待机中" to Color(0xFF9E9E9E)
                TimerState.RUNNING -> "使用中" to Color(0xFF66BB6A)
                TimerState.WARNING -> "即将锁定" to Color(0xFFFFA726)
                TimerState.LOCKED -> "已锁定" to Color(0xFFEF5350)
            }

            Text(
                text = statusText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 状态详情
            when (state) {
                TimerState.IDLE -> {
                    Text(
                        text = "已设置 $totalMinutes 分钟观看时长",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TimerState.RUNNING -> {
                    Text(
                        text = "剩余使用时间：$remaining",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TimerState.WARNING -> {
                    Text(
                        text = "即将锁定，剩余 $remaining",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFA726)
                    )
                }
                TimerState.LOCKED -> {
                    Text(
                        text = "请打开锁定屏幕输入密码",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
