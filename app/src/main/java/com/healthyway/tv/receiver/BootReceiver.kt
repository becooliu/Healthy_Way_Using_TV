package com.healthyway.tv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.healthyway.tv.data.PreferencesManager
import com.healthyway.tv.service.TimerService

/**
 * 开机广播接收器
 * 【功能】系统启动后检查是否有未完成的计时任务，自动恢复计时或锁定
 * 【说明】需要 RECEIVE_BOOT_COMPLETED 权限
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "系统启动完成，检查计时状态...")

            val preferencesManager = PreferencesManager.getInstance(context)

            // 如果有锁定状态或未完成的计时，启动服务自动处理
            if (preferencesManager.isLocked || preferencesManager.isTimerRunning) {
                Log.d(TAG, "检测到未完成状态（locked=${preferencesManager.isLocked}, running=${preferencesManager.isTimerRunning}），正在恢复...")
                TimerService.startTimer(context)
            }
        }
    }
}
