package com.healthyway.tv.ui.lock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthyway.tv.data.PreferencesManager
import com.healthyway.tv.service.TimerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 锁定屏幕 ViewModel
 * 【功能】管理锁定界面的密码验证和自动解锁倒计时
 * 【高内聚】锁定相关的所有逻辑集中在此类中
 */
class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager.getInstance(application)

    /** 锁定界面 UI 状态 */
    data class LockUiState(
        /** 密码输入 */
        val passwordInput: String = "",
        /** 错误提示信息 */
        val errorMessage: String? = null,
        /** 是否已解锁 */
        val isUnlocked: Boolean = false,
        /** 自动解锁剩余秒数 */
        val autoUnlockRemainingSeconds: Int = 20 * 60, // 默认20分钟
        /** 是否正在验证密码 */
        val isVerifying: Boolean = false
    )

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState

    /** 自动解锁计时任务 */
    private var autoUnlockJob: Job? = null

    init {
        startAutoUnlockCountdown()
    }

    /**
     * 处理密码输入
     * @param digit 输入的数字字符
     */
    fun onDigitInput(digit: Char) {
        val current = _uiState.value.passwordInput
        if (current.length < 6) {
            val newInput = current + digit
            _uiState.value = _uiState.value.copy(
                passwordInput = newInput,
                errorMessage = null
            )
            // 输入满4位后自动验证
            if (newInput.length >= 4) {
                verifyPassword()
            }
        }
    }

    /**
     * 删除最后一个输入的数字
     */
    fun onDeleteLast() {
        val current = _uiState.value.passwordInput
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                passwordInput = current.dropLast(1),
                errorMessage = null
            )
        }
    }

    /**
     * 清除输入
     */
    fun onClear() {
        _uiState.value = _uiState.value.copy(
            passwordInput = "",
            errorMessage = null
        )
    }

    /**
     * 验证密码
     * 【流程】哈希输入 → 与存储的哈希比较 → 成功/失败处理
     */
    private fun verifyPassword() {
        val input = _uiState.value.passwordInput
        if (input.length < 4) return

        val storedHash = preferencesManager.passwordHash
        if (storedHash.isEmpty()) {
            // 未设置密码，直接解锁
            unlock()
            return
        }

        _uiState.value = _uiState.value.copy(isVerifying = true)

        val inputHash = PreferencesManager.hashPassword(input)
        if (inputHash == storedHash) {
            unlock()
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "密码错误，请重试",
                passwordInput = "",
                isVerifying = false
            )
        }
    }

    /**
     * 执行解锁操作
     */
    private fun unlock() {
        autoUnlockJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isUnlocked = true,
            isVerifying = false
        )
        // 通知服务解锁
        TimerService.unlock(getApplication())
    }

    /**
     * 启动自动解锁倒计时（20分钟）
     * 【功能】倒计时结束后自动解锁屏幕
     */
    private fun startAutoUnlockCountdown() {
        autoUnlockJob?.cancel()
        autoUnlockJob = viewModelScope.launch {
            var remaining = 20 * 60 // 20分钟 = 1200秒
            while (remaining > 0) {
                _uiState.value = _uiState.value.copy(
                    autoUnlockRemainingSeconds = remaining
                )
                delay(1000L)
                remaining--
            }
            // 倒计时结束，自动解锁
            unlock()
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoUnlockJob?.cancel()
    }
}
