package com.example.simpleschedule.domain.model

import androidx.compose.ui.graphics.Color

/**
 * 课程领域模型
 *
 * 纯数据类，不依赖任何持久化框架。
 * 颜色使用两种形式存储：
 * - color: Compose Color（用于 UI 渲染）
 * - colorValue: Long ARGB（用于 JSON 序列化）
 */
data class Course(
    val id: Long = 0,
    val courseName: String,
    val teacher: String = "",
    val classroom: String = "",
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: WeekType = WeekType.ALL,
    val colorValue: Long = 0xFFA3B5A6  // ARGB Long 值，用于序列化
) {
    /** Compose Color 表示，从 colorValue 懒计算 */
    val color: Color
        get() = Color(colorValue.toInt())
}

/**
 * 课程周类型枚举
 *
 * ALL  — 每周上课
 * ODD  — 仅在奇数周上课（第 1, 3, 5... 周）
 * EVEN — 仅在偶数周上课（第 2, 4, 6... 周）
 */
enum class WeekType(val label: String, val dbValue: String) {
    ALL("所有周", "ALL"),
    ODD("单周", "ODD"),
    EVEN("双周", "EVEN");

    companion object {
        /** 从存储值恢复枚举 */
        fun fromDbValue(value: String): WeekType =
            entries.firstOrNull { it.dbValue == value } ?: ALL
    }
}
