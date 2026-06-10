package com.healthyway.tv.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.security.MessageDigest

/**
 * 首选项管理器 - 单例模式
 * 【功能】统一管理应用配置数据的持久化存储和读取
 * 【高内聚】所有数据存取操作集中在此类中
 */
class PreferencesManager private constructor(context: Context) {

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREF_NAME = "healthy_tv_prefs"

        // SharedPreferences 键名常量（集中管理，避免散落各处）
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_WARNING = "warning_minutes"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_TIMER_RUNNING = "timer_running"
        private const val KEY_TIMER_START = "timer_start_ms"
        private const val KEY_TIMER_DURATION = "timer_saved_duration_ms"
        private const val KEY_TIMER_WARNING = "timer_saved_warning_ms"
        private const val KEY_IS_LOCKED = "is_locked"

        /** 哈希算法 */
        private const val HASH_ALGORITHM = "SHA-256"

        @Volatile
        private var instance: PreferencesManager? = null

        /**
         * 获取 PreferencesManager 单例
         * @param context Application Context
         * @return 单例实例
         */
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * 密码哈希工具方法
         * @param password 明文密码（4-6位数字）
         * @return SHA-256 哈希值（十六进制字符串）
         */
        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ==================== 用户配置 ====================

    /** 获取/设置每次观看时长（分钟） */
    var durationMinutes: Int
        get() = prefs.getInt(KEY_DURATION, 30)
        set(value) = prefs.edit().putInt(KEY_DURATION, value.coerceIn(1, 480)).apply()

    /** 获取/设置提前提醒时间（分钟） */
    var warningMinutes: Int
        get() = prefs.getInt(KEY_WARNING, 2)
        set(value) = prefs.edit().putInt(KEY_WARNING, value.coerceIn(1, 30)).apply()

    /** 获取/设置密码哈希值 */
    var passwordHash: String
        get() = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD_HASH, value).apply()

    /** 获取完整配置 */
    fun getConfig(): TimerConfig {
        return TimerConfig(
            durationMinutes = durationMinutes,
            warningMinutes = warningMinutes,
            passwordHash = passwordHash
        )
    }

    /**
     * 保存完整配置
     * @param config 计时配置
     */
    fun saveConfig(config: TimerConfig) {
        durationMinutes = config.durationMinutes
        warningMinutes = config.warningMinutes
        passwordHash = config.passwordHash
        Log.d(TAG, "配置已保存: duration=${config.durationMinutes}, warning=${config.warningMinutes}")
    }

    // ==================== 运行状态（用于系统重启恢复） ====================

    /** 计时是否正在运行 */
    var isTimerRunning: Boolean
        get() = prefs.getBoolean(KEY_TIMER_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_TIMER_RUNNING, value).apply()

    /** 计时开始时间戳（毫秒） */
    var timerStartMs: Long
        get() = prefs.getLong(KEY_TIMER_START, 0L)
        set(value) = prefs.edit().putLong(KEY_TIMER_START, value).apply()

    /** 计时的总时长（毫秒） */
    var timerSavedDurationMs: Long
        get() = prefs.getLong(KEY_TIMER_DURATION, 0L)
        set(value) = prefs.edit().putLong(KEY_TIMER_DURATION, value).apply()

    /** 计时的提醒阈值（毫秒） */
    var timerSavedWarningMs: Long
        get() = prefs.getLong(KEY_TIMER_WARNING, 0L)
        set(value) = prefs.edit().putLong(KEY_TIMER_WARNING, value).apply()

    /** 是否处于锁定状态 */
    var isLocked: Boolean
        get() = prefs.getBoolean(KEY_IS_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOCKED, value).apply()

    /**
     * 保存计时器运行状态（用于恢复）
     * @param startMs 开始时间戳
     * @param durationMs 总时长
     * @param warningMs 提醒阈值
     */
    fun saveTimerRunning(startMs: Long, durationMs: Long, warningMs: Long) {
        prefs.edit()
            .putBoolean(KEY_TIMER_RUNNING, true)
            .putLong(KEY_TIMER_START, startMs)
            .putLong(KEY_TIMER_DURATION, durationMs)
            .putLong(KEY_TIMER_WARNING, warningMs)
            .apply()
    }

    /** 清除计时器运行状态 */
    fun clearTimerState() {
        prefs.edit()
            .putBoolean(KEY_TIMER_RUNNING, false)
            .putLong(KEY_TIMER_START, 0L)
            .putLong(KEY_TIMER_DURATION, 0L)
            .putLong(KEY_TIMER_WARNING, 0L)
            .putBoolean(KEY_IS_LOCKED, false)
            .apply()
    }

    /** 清除所有设置 */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
