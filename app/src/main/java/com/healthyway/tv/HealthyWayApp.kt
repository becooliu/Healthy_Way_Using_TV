package com.healthyway.tv

import android.app.Application
import android.util.Log
import com.healthyway.tv.data.PreferencesManager

/**
 * Application 入口类
 * 【功能】应用初始化入口，全局配置
 */
class HealthyWayApp : Application() {

    companion object {
        private const val TAG = "HealthyWayApp"
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化 PreferencesManager 单例
        PreferencesManager.getInstance(this)
        Log.d(TAG, "应用已启动")
    }
}
