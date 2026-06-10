package com.healthyway.tv.data

/**
 * 计时器配置数据类
 * 【功能】保存家长设定的各项计时参数
 * 【高内聚】所有计时相关配置集中在单一数据类中
 */
data class TimerConfig(
    /** 每次允许观看的总时长（分钟），范围 1-480 */
    val durationMinutes: Int = 30,
    /** 提前提醒时间（分钟），范围 1-30，默认 2 分钟 */
    val warningMinutes: Int = 2,
    /** 锁定密码（SHA-256 哈希值），4-6 位数字 */
    val passwordHash: String = "",
    /** 自动解锁等待时间（分钟），固定 20 分钟 */
    val autoUnlockMinutes: Int = 20
)
