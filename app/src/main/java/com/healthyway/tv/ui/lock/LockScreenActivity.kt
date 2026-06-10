package com.healthyway.tv.ui.lock

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthyway.tv.service.TimerService
import com.healthyway.tv.ui.theme.HealthyWayTheme

/**
 * 锁定屏幕 Activity
 * 【功能】计时到期时全屏启动，阻止用户继续使用电视
 * 【说明】
 *   - 拦截返回键和菜单键防止退出
 *   - 接收解锁广播后自动关闭
 *   - 不可从最近任务列表访问
 */
class LockScreenActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LockScreenActivity"
    }

    private lateinit var localBroadcastManager: LocalBroadcastManager

    /** 监听解锁和停止广播，自动关闭 */
    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                TimerService.BROADCAST_UNLOCKED,
                TimerService.BROADCAST_STOPPED -> {
                    if (!isFinishing) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // 注册自动关闭广播接收器
        val filter = IntentFilter().apply {
            addAction(TimerService.BROADCAST_UNLOCKED)
            addAction(TimerService.BROADCAST_STOPPED)
        }
        localBroadcastManager.registerReceiver(finishReceiver, filter)

        setContent {
            HealthyWayTheme {
                val viewModel: LockViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                // 解锁成功后自动关闭
                LaunchedEffect(uiState.isUnlocked) {
                    if (uiState.isUnlocked) {
                        finish()
                    }
                }

                LockScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(finishReceiver)
    }

    // ==================== 按键拦截 ====================

    /** 拦截返回键，防止用户退出锁定屏幕 */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 阻止返回键和主页键
        if (keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_HOME ||
            keyCode == KeyEvent.KEYCODE_MENU
        ) {
            return true // 消费事件，不执行默认操作
        }
        return super.onKeyDown(keyCode, event)
    }

    /** 防止 Activity 被其他方式关闭 */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // 用户试图离开时，重新打开锁屏
        if (!isFinishing) {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }
}
