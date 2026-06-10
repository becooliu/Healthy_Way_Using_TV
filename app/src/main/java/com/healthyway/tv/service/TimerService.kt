package com.healthyway.tv.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.healthyway.tv.data.PreferencesManager
import com.healthyway.tv.data.TimerState
import com.healthyway.tv.ui.lock.LockScreenActivity
import kotlinx.coroutines.*

/**
 * 计时前台服务
 * 【功能】在后台运行计时器，到期触发锁定屏幕
 * 【高内聚】所有计时逻辑集中在此服务中
 * 
 * 工作流程：
 *  1. 接收启动命令，开始倒计时
 *  2. 到达提醒阈值 → 发送警告广播 + 显示警告通知
 *  3. 计时归零 → 启动锁定屏幕 Activity
 *  4. 接收解锁/停止命令 → 停止服务并清理状态
 */
class TimerService : Service() {

    companion object {
        private const val TAG = "TimerService"

        // Intent Action 常量
        const val ACTION_START_TIMER = "com.healthyway.tv.action.START_TIMER"
        const val ACTION_STOP_TIMER = "com.healthyway.tv.action.STOP_TIMER"
        const val ACTION_UNLOCK = "com.healthyway.tv.action.UNLOCK"
        const val ACTION_UPDATE_CONFIG = "com.healthyway.tv.action.UPDATE_CONFIG"

        // 广播 Action 常量
        const val BROADCAST_TICK = "com.healthyway.tv.broadcast.TICK"
        const val BROADCAST_WARNING = "com.healthyway.tv.broadcast.WARNING"
        const val BROADCAST_LOCKED = "com.healthyway.tv.broadcast.LOCKED"
        const val BROADCAST_UNLOCKED = "com.healthyway.tv.broadcast.UNLOCKED"
        const val BROADCAST_STOPPED = "com.healthyway.tv.broadcast.STOPPED"

        // Extra 键名
        const val EXTRA_REMAINING_MS = "remaining_ms"
        const val EXTRA_TOTAL_MS = "total_ms"
        const val EXTRA_STATE = "state"

        /**
         * 启动计时的便捷方法
         * @param context Context
         */
        fun startTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START_TIMER
            }
            context.startForegroundService(intent)
        }

        /**
         * 停止计时的便捷方法
         * @param context Context
         */
        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
            context.startService(intent)
        }

        /**
         * 发送解锁命令
         * @param context Context
         */
        fun unlock(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_UNLOCK
            }
            context.startService(intent)
        }
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationHelper: TimerNotificationHelper

    /** 协程作用域，用于管理计时任务 */
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** 计时任务 Job，用于取消计时 */
    private var timerJob: Job? = null

    /** 当前计时状态 */
    private var currentState: TimerState = TimerState.IDLE

    /** 是否已发送过警告 */
    private var warningSent = false

    /** 总计时长（毫秒） */
    private var totalDurationMs: Long = 0L
    /** 提醒阈值（毫秒） */
    private var warningThresholdMs: Long = 0L

    /** 本地广播管理器，用于与 Activity 通信 */
    private lateinit var localBroadcastManager: LocalBroadcastManager

    // ==================== 服务生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager.getInstance(this)
        notificationHelper = TimerNotificationHelper(this)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        notificationHelper.createNotificationChannel()
        Log.d(TAG, "计时服务已创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> handleStartTimer()
            ACTION_STOP_TIMER -> handleStopTimer()
            ACTION_UNLOCK -> handleUnlock()
            ACTION_UPDATE_CONFIG -> handleUpdateConfig()
            else -> {
                // 如果服务因系统重启而恢复，检查是否需要继续计时或锁定
                if (preferencesManager.isLocked) {
                    Log.d(TAG, "检测到锁定状态，正在恢复...")
                    handleTimerExpired()
                } else if (preferencesManager.isTimerRunning && preferencesManager.timerStartMs > 0) {
                    Log.d(TAG, "检测到未完成的计时任务，尝试恢复...")
                    handleStartTimer()
                }
            }
        }
        return START_STICKY // 服务被杀死后尝试重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        notificationHelper.cancelAll()
        Log.d(TAG, "计时服务已销毁")
        super.onDestroy()
    }

    // ==================== 核心逻辑 ====================

    /**
     * 处理启动计时命令
     * 【流程】读取配置 → 启动前台服务 → 开始倒计时
     */
    private fun handleStartTimer() {
        // 如果已有计时任务，先取消
        timerJob?.cancel()

        val config = preferencesManager.getConfig()

        // 计算总时长和提醒阈值
        totalDurationMs = config.durationMinutes * 60 * 1000L
        warningThresholdMs = config.warningMinutes * 60 * 1000L

        // 检查是否有未完成的计时（系统重启恢复）
        var remainingMs = totalDurationMs
        if (preferencesManager.isTimerRunning && preferencesManager.timerStartMs > 0) {
            val elapsedMs = System.currentTimeMillis() - preferencesManager.timerStartMs
            remainingMs = (preferencesManager.timerSavedDurationMs - elapsedMs).coerceAtLeast(0L)
            totalDurationMs = preferencesManager.timerSavedDurationMs
            warningThresholdMs = preferencesManager.timerSavedWarningMs
        }

        // 如果剩余时间 <= 0，直接锁定
        if (remainingMs <= 0) {
            handleTimerExpired()
            return
        }

        // 保存状态
        preferencesManager.saveTimerRunning(
            System.currentTimeMillis(),
            totalDurationMs,
            warningThresholdMs
        )

        // 启动前台服务
        val remainingMinutes = (remainingMs / 60000).toInt()
        startForeground(
            TimerNotificationHelper.NOTIFICATION_ID,
            notificationHelper.buildNotification(remainingMinutes, false)
        )

        warningSent = false
        currentState = TimerState.RUNNING
        broadcastState()

        // 启动计时协程
        timerJob = serviceScope.launch {
            runTimer(remainingMs)
        }

        Log.d(TAG, "计时已启动: 总时长=${totalDurationMs}ms, 剩余=${remainingMs}ms")
    }

    /**
     * 倒计时主循环
     * 【功能】每秒更新一次，检查是否到达提醒阈值或计时结束
     * @param initialRemainingMs 初始剩余毫秒数
     */
    private suspend fun runTimer(initialRemainingMs: Long) {
        var remainingMs = initialRemainingMs

        while (remainingMs > 0 && isActive) {
            // 计算当前状态
            if (remainingMs <= warningThresholdMs && !warningSent) {
                warningSent = true
                currentState = TimerState.WARNING
                // 发送警告广播（主线程）
                withContext(Dispatchers.Main) {
                    sendWarningBroadcast(remainingMs)
                    notificationHelper.showWarningNotification(
                        (remainingMs / 60000).toInt() + 1
                    )
                }
            }

            // 更新通知
            val remainingMinutes = (remainingMs / 60000) + 1
            val isWarning = currentState == TimerState.WARNING
            withContext(Dispatchers.Main) {
                val notification = notificationHelper.buildNotification(
                    remainingMinutes.coerceAtLeast(1), isWarning
                )
                // 更新前台服务通知
                startForeground(TimerNotificationHelper.NOTIFICATION_ID, notification)

                // 发送心跳广播
                sendTickBroadcast(remainingMs)
            }

            // 等待 1 秒
            delay(1000L)
            remainingMs -= 1000L
        }

        // 倒计时结束，处理超时
        if (isActive) {
            withContext(Dispatchers.Main) {
                handleTimerExpired()
            }
        }
    }

    /**
     * 处理计时到期
     * 【功能】启动锁定屏幕，更新状态
     */
    private fun handleTimerExpired() {
        currentState = TimerState.LOCKED
        preferencesManager.isLocked = true
        broadcastState()
        sendLockedBroadcast()

        // 更新通知为锁定状态（保持前台服务运行以接收解锁命令）
        val lockedNotification = notificationHelper.buildLockedNotification()
        startForeground(TimerNotificationHelper.NOTIFICATION_ID, lockedNotification)

        // 启动锁定屏幕 Activity
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(lockIntent)

        Log.d(TAG, "计时到期，屏幕已锁定")
    }

    /**
     * 处理停止计时命令
     */
    private fun handleStopTimer() {
        timerJob?.cancel()
        stopForeground(true)
        preferencesManager.clearTimerState()
        currentState = TimerState.IDLE
        broadcastState()
        sendStoppedBroadcast()
        stopSelf()
        Log.d(TAG, "计时已停止")
    }

    /**
     * 处理解锁命令（密码正确或自动解锁）
     */
    private fun handleUnlock() {
        timerJob?.cancel()
        stopForeground(true)
        preferencesManager.clearTimerState()
        currentState = TimerState.IDLE
        broadcastState()
        sendUnlockedBroadcast()
        stopSelf()
        Log.d(TAG, "屏幕已解锁")
    }

    /**
     * 处理配置更新
     */
    private fun handleUpdateConfig() {
        // 如果计时正在运行，不中断，下次启动使用新配置
        Log.d(TAG, "配置已更新（下次生效）")
    }

    // ==================== 广播发送 ====================

    /**
     * 发送计时状态广播
     */
    private fun broadcastState() {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra(EXTRA_STATE, currentState.name)
            putExtra(EXTRA_REMAINING_MS, 0L)
            putExtra(EXTRA_TOTAL_MS, totalDurationMs)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    /**
     * 发送心跳广播（每秒一次）
     */
    private fun sendTickBroadcast(remainingMs: Long) {
        val intent = Intent(BROADCAST_TICK).apply {
            putExtra(EXTRA_STATE, currentState.name)
            putExtra(EXTRA_REMAINING_MS, remainingMs)
            putExtra(EXTRA_TOTAL_MS, totalDurationMs)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    /**
     * 发送警告广播
     */
    private fun sendWarningBroadcast(remainingMs: Long) {
        val intent = Intent(BROADCAST_WARNING).apply {
            putExtra(EXTRA_REMAINING_MS, remainingMs)
        }
        localBroadcastManager.sendBroadcast(intent)
    }

    /**
     * 发送锁定广播
     */
    private fun sendLockedBroadcast() {
        localBroadcastManager.sendBroadcast(Intent(BROADCAST_LOCKED))
    }

    /**
     * 发送解锁广播
     */
    private fun sendUnlockedBroadcast() {
        localBroadcastManager.sendBroadcast(Intent(BROADCAST_UNLOCKED))
    }

    /**
     * 发送停止广播
     */
    private fun sendStoppedBroadcast() {
        localBroadcastManager.sendBroadcast(Intent(BROADCAST_STOPPED))
    }
}
