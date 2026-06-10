package com.healthyway.tv.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.healthyway.tv.data.PreferencesManager
import com.healthyway.tv.data.TimerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 设置界面 ViewModel
 * 【功能】管理设置界面的数据和用户交互逻辑
 * 【低耦合】通过 PreferencesManager 与数据层交互，不直接操作持久化
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager.getInstance(application)

    /** 设置界面 UI 状态 */
    data class SettingsUiState(
        /** 每次观看时长（分钟） */
        val durationMinutes: String = "30",
        /** 提前提醒时间（分钟） */
        val warningMinutes: String = "2",
        /** 密码输入 */
        val password: String = "",
        /** 确认密码 */
        val confirmPassword: String = "",
        /** 是否已设置密码 */
        val hasPassword: Boolean = false,
        /** 错误提示信息 */
        val errorMessage: String? = null,
        /** 成功提示信息 */
        val successMessage: String? = null,
        /** 保存中标志 */
        val isSaving: Boolean = false
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadConfig()
    }

    /** 从持久化存储加载配置 */
    private fun loadConfig() {
        val config = preferencesManager.getConfig()
        _uiState.value = _uiState.value.copy(
            durationMinutes = config.durationMinutes.toString(),
            warningMinutes = config.warningMinutes.toString(),
            hasPassword = config.passwordHash.isNotEmpty(),
            password = "",
            confirmPassword = ""
        )
    }

    /** 更新时长 */
    fun onDurationChange(value: String) {
        // 只允许数字输入
        if (value.all { it.isDigit() } || value.isEmpty()) {
            _uiState.value = _uiState.value.copy(durationMinutes = value, errorMessage = null)
        }
    }

    /** 更新提醒时间 */
    fun onWarningChange(value: String) {
        if (value.all { it.isDigit() } || value.isEmpty()) {
            _uiState.value = _uiState.value.copy(warningMinutes = value, errorMessage = null)
        }
    }

    /** 更新密码 */
    fun onPasswordChange(value: String) {
        // 只允许数字，最多6位
        if (value.all { it.isDigit() } && value.length <= 6) {
            _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
        }
    }

    /** 更新确认密码 */
    fun onConfirmPasswordChange(value: String) {
        if (value.all { it.isDigit() } && value.length <= 6) {
            _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
        }
    }

    /**
     * 保存设置
     * 【流程】校验输入 → 保存到 SharedPreferences → 反馈结果
     */
    fun saveSettings() {
        val state = _uiState.value

        // 校验时长
        val duration = state.durationMinutes.toIntOrNull()
        if (duration == null || duration < 1 || duration > 480) {
            _uiState.value = state.copy(errorMessage = "观看时长须在 1~480 分钟之间")
            return
        }

        // 校验提醒时间
        val warning = state.warningMinutes.toIntOrNull()
        if (warning == null || warning < 1 || warning > 30) {
            _uiState.value = state.copy(errorMessage = "提醒时间须在 1~30 分钟之间")
            return
        }

        // 校验提醒时间不能大于总时长
        if (warning >= duration) {
            _uiState.value = state.copy(errorMessage = "提醒时间不能大于或等于观看时长")
            return
        }

        // 校验密码一致性
        if (state.password.isNotEmpty() || state.confirmPassword.isNotEmpty()) {
            if (state.password.length < 4) {
                _uiState.value = state.copy(errorMessage = "密码至少需要4位数字")
                return
            }
            if (state.password != state.confirmPassword) {
                _uiState.value = state.copy(errorMessage = "两次输入的密码不一致")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            // 构建配置并保存
            val config = TimerConfig(
                durationMinutes = duration,
                warningMinutes = warning,
                passwordHash = if (state.password.isNotEmpty()) {
                    PreferencesManager.hashPassword(state.password)
                } else {
                    preferencesManager.passwordHash // 保留现有密码
                }
            )
            preferencesManager.saveConfig(config)

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                successMessage = "设置已保存",
                hasPassword = config.passwordHash.isNotEmpty(),
                password = "",
                confirmPassword = ""
            )
        }
    }

    /** 清除错误提示 */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /** 清除成功提示 */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
