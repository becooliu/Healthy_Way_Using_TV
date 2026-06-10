package com.healthyway.tv.ui.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设置界面 Composable
 * 【功能】提供观看时长、提醒时间、密码等参数的设置界面
 * 【说明】适配 Android TV 遥控器 D-pad 焦点导航
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "使用设置",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 观看时长输入
        SettingField(
            label = "每次观看时长（分钟）",
            value = uiState.durationMinutes,
            onValueChange = viewModel::onDurationChange,
            hint = "1~480",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 提醒时间输入
        SettingField(
            label = "提前提醒时间（分钟）",
            value = uiState.warningMinutes,
            onValueChange = viewModel::onWarningChange,
            hint = "1~30，默认 2",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 密码输入
        SettingField(
            label = "锁定密码（4-6位数字）",
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            hint = if (uiState.hasPassword) "留空则使用现有密码" else "请输入4-6位数字密码",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 确认密码输入
        SettingField(
            label = "确认密码",
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            hint = "请再次输入密码",
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 错误提示
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 成功提示
        if (uiState.successMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.successMessage!!,
                color = Color(0xFF66BB6A),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 保存按钮
        Button(
            onClick = viewModel::saveSettings,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .width(280.dp)
                .height(56.dp)
                .focusable(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "保存设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 设置输入字段组件
 * @param label 字段标签
 * @param value 当前值
 * @param onValueChange 值变化回调
 * @param hint 占位提示
 * @param isPassword 是否密码输入
 * @param modifier Modifier
 */
@Composable
private fun SettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = hint,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusable(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
