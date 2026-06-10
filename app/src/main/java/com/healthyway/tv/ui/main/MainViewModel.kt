package com.healthyway.tv.ui.main

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.healthyway.tv.data.PreferencesManager
import com.healthyway.tv.data.TimerConfig
import com.healthyway.tv.data.TimerState
import com.healthyway.tv.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 主界面 ViewModel
 * 【功能】管理主页面的状态，协调计时服务与 UI 的通信
 * 【低耦合】通过广播接收器与 TimerService 通信
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager.getInstance(application)
    private val localBroadcastManager = LocalBroadcastManager.getInstance(application)

    /** 主界面 UI 状态 */
    data class MainUiState(
        /** 当前计时状态 */
        val timerState: TimerState = TimerState.IDLE,
        /** 剩余毫秒数 */
        val remainingMs: Long = 0L,
        /** 总毫秒数 */
        val totalMs: Long = 0L,
        /** 当前配置 */
        val config: TimerConfig = TimerConfig(),
        /** 是否显示设置界面 */
        val showSettings: Boolean = false
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    /** 本地广播接收器 */
    private val timerBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                TimerService.BROADCAST_TICK -> {
                    val stateName = intent.getStringExtra(TimerService.EXTRA_STATE) ?: return
                    val remainingMs = intent.getLongExtra(TimerService.EXTRA_REMAINING_MS, 0L)
                    val totalMs = intent.getLongExtra(TimerService.EXTRA_TOTAL_MS, 0L)
                    _uiState.value = _uiState.value.copy(
                        timerState = TimerState.valueOf(stateName),
                        remainingMs = remainingMs,
                        totalMs = totalMs
                    )
                }
                TimerService.BROADCAST_LOCKED -> {
                    _uiState.value = _uiState.value.copy(
                        timerState = TimerState.LOCKED
                    )
                }
                TimerService.BROADCAST_UNLOCKED, TimerService.BROADCAST_STOPPED -> {
                    _uiState.value = _uiState.value.copy(
                        timerState = TimerState.IDLE,
                        remainingMs = 0L,
                        totalMs = 0L
                    )
                }
                TimerService.BROADCAST_WARNING -> {
                    _uiState.value = _uiState.value.copy(
                        timerState = TimerState.WARNING
                    )
                }
            }
        }
    }

    init {
        loadConfig()
        registerBroadcastReceiver()
    }

    /** 注册本地广播接收器 */
    private fun registerBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(TimerService.BROADCAST_TICK)
            addAction(TimerService.BROADCAST_WARNING)
            addAction(TimerService.BROADCAST_LOCKED)
            addAction(TimerService.BROADCAST_UNLOCKED)
            addAction(TimerService.BROADCAST_STOPPED)
        }
        localBroadcastManager.registerReceiver(timerBroadcastReceiver, filter)
    }

    /** 加载配置 */
    private fun loadConfig() {
        _uiState.value = _uiState.value.copy(
            config = preferencesManager.getConfig()
        )
    }

    /** 切换设置界面显示 */
    fun toggleSettings() {
        _uiState.value = _uiState.value.copy(
            showSettings = !_uiState.value.showSettings
        )
    }

    /** 刷新配置 */
    fun refreshConfig() {
        loadConfig()
    }

    /**
     * 启动计时
     * 【流程】检查密码是否已设置 → 启动服务
     */
    fun startTimer() {
        val config = _uiState.value.config
        if (config.passwordHash.isEmpty()) {
            // 需要先设置密码
            _uiState.value = _uiState.value.copy(showSettings = true)
            return
        }
        TimerService.startTimer(getApplication())
    }

    /** 停止计时 */
    fun stopTimer() {
        TimerService.stopTimer(getApplication())
    }

    /**
     * 获取格式化剩余时间
     * @return "X分钟" 格式字符串
     */
    fun getFormattedRemaining(): String {
        val ms = _uiState.value.remainingMs
        return if (ms > 0) {
            "${(ms / 60000) + 1}分钟"
        } else {
            "--"
        }
    }

    override fun onCleared() {
        super.onCleared()
        localBroadcastManager.unregisterReceiver(timerBroadcastReceiver)
    }
}
