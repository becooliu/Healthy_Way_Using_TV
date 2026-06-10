package com.healthyway.tv.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 锁定屏幕 Composable
 * 【功能】计时到期后全屏显示，阻止继续观看
 * 【说明】提供密码输入和自动解锁倒计时显示
 */
@Composable
fun LockScreen(
    viewModel: LockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 锁定图标和标题
        Text(
            text = "屏幕已锁定",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEF5350),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "使用时间已到，请休息一下",
            fontSize = 22.sp,
            color = Color(0xFFBDBDBD),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 密码提示
        Text(
            text = "输入密码解锁",
            fontSize = 20.sp,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 密码圆点显示
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val isFilled = i < uiState.passwordInput.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) MaterialTheme.colorScheme.primary
                            else Color(0xFF424242)
                        )
                )
            }
        }

        // 错误提示
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.errorMessage!!,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 数字键盘
        NumberPad(
            onDigitClick = viewModel::onDigitInput,
            onDelete = viewModel::onDeleteLast,
            onClear = viewModel::onClear
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 自动解锁倒计时
        val minutes = uiState.autoUnlockRemainingSeconds / 60
        val seconds = uiState.autoUnlockRemainingSeconds % 60
        Text(
            text = "不输入密码时，${minutes}分${seconds}秒后自动解锁",
            fontSize = 18.sp,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 数字键盘组件
 * 【功能】提供 0-9 数字键、删除键和清除键，适配遥控器操作
 */
@Composable
private fun NumberPad(
    onDigitClick: (Char) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一行：1 2 3
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberKey('1', onClick = { onDigitClick('1') })
            NumberKey('2', onClick = { onDigitClick('2') })
            NumberKey('3', onClick = { onDigitClick('3') })
        }
        // 第二行：4 5 6
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberKey('4', onClick = { onDigitClick('4') })
            NumberKey('5', onClick = { onDigitClick('5') })
            NumberKey('6', onClick = { onDigitClick('6') })
        }
        // 第三行：7 8 9
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberKey('7', onClick = { onDigitClick('7') })
            NumberKey('8', onClick = { onDigitClick('8') })
            NumberKey('9', onClick = { onDigitClick('9') })
        }
        // 第四行：清除 0 删除
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // 清除按钮
            ActionKey(
                label = "清除",
                onClick = onClear,
                color = Color(0xFF757575)
            )
            // 数字 0
            NumberKey('0', onClick = { onDigitClick('0') })
            // 删除按钮
            ActionKey(
                label = "删除",
                onClick = onDelete,
                color = Color(0xFF757575)
            )
        }
    }
}

/**
 * 数字按键组件
 */
@Composable
private fun NumberKey(
    digit: Char,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFF424242), RoundedCornerShape(12.dp))
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * 功能按键组件（删除/清除）
 */
@Composable
private fun ActionKey(
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, color, RoundedCornerShape(12.dp))
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
