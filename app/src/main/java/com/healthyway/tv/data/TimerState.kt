package com.healthyway.tv.data

/**
 * 计时器运行状态枚举
 * 【功能】表示计时器的生命周期状态
 */
enum class TimerState {
    /** 待机状态：未开始计时 */
    IDLE,
    /** 运行中：正在正常计时 */
    RUNNING,
    /** 警告阶段：已进入提前提醒区间 */
    WARNING,
    /** 已锁定：使用时间已到 */
    LOCKED
}
