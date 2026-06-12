package com.example.simpleschedule.util

import com.example.simpleschedule.domain.model.WeekType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 教学周计算工具类
 *
 * 核心逻辑：
 * 1. 根据学期第一周周一日期，计算当前日期所处的教学周
 * 2. 判断指定周是否匹配课程的单双周类型
 *
 * 算法：
 *   weekNumber = floor((今天 - 学期开始日期) / 7) + 1
 *   如果当前日期在学期开始之前，默认返回第 1 周
 */
object WeekCalculator {

    /**
     * 计算当前日期对应的教学周数
     *
     * @param semesterStartDate 学期第一周的周一日期
     * @param currentDate 当前日期（默认为今天）
     * @return 教学周数（从 1 开始）
     *
     * 示例：学期 2月24日（周一）开始，3月3日是第 2 周
     */
    fun calculateCurrentWeek(
        semesterStartDate: LocalDate,
        currentDate: LocalDate = LocalDate.now()
    ): Int {
        val daysSinceStart = ChronoUnit.DAYS.between(semesterStartDate, currentDate)
        // 如果今天在学期开始之前，返回第 1 周
        if (daysSinceStart < 0) return 1
        return (daysSinceStart / 7).toInt() + 1
    }

    /**
     * 判断指定周数是否匹配课程要求的周类型
     *
     * @param weekNumber 当前显示的教学周数
     * @param weekType 课程的周类型要求
     * @return true 表示该课程在本周应显示
     *
     * 规则：
     * - ALL：  每周都显示
     * - ODD：  仅在奇数周显示（第 1, 3, 5, 7... 周）
     * - EVEN： 仅在偶数周显示（第 2, 4, 6, 8... 周）
     */
    fun matchesWeekType(weekNumber: Int, weekType: WeekType): Boolean = when (weekType) {
        WeekType.ALL  -> true
        WeekType.ODD  -> weekNumber % 2 == 1  // 奇数 = 单周
        WeekType.EVEN -> weekNumber % 2 == 0  // 偶数 = 双周
    }

    /**
     * 计算指定教学周的第一天（周一）日期
     *
     * @param semesterStartDate 学期第一周周一
     * @param weekNumber 目标教学周（1-based）
     * @return 该周周一的日期
     */
    fun mondayOfWeek(semesterStartDate: LocalDate, weekNumber: Int): LocalDate =
        semesterStartDate.plusWeeks((weekNumber - 1).toLong())

    /**
     * 格式化周日期范围，如 "2/24-2/28"
     *
     * @param semesterStartDate 学期第一周周一
     * @param weekNumber 教学周数
     * @return 该周周一~周五的日期范围字符串
     */
    fun weekDateRange(semesterStartDate: LocalDate, weekNumber: Int): String {
        val monday = mondayOfWeek(semesterStartDate, weekNumber)
        val friday = monday.plusDays(4)
        val fmt = java.time.format.DateTimeFormatter.ofPattern("M/d")
        return "${monday.format(fmt)}-${friday.format(fmt)}"
    }

    /**
     * 获取某周每一天的日期字符串（如 "3/24"），索引 0=周一, 6=周日
     */
    fun dayDates(semesterStartDate: LocalDate, weekNumber: Int): List<String> {
        val monday = mondayOfWeek(semesterStartDate, weekNumber)
        val fmt = java.time.format.DateTimeFormatter.ofPattern("M/d")
        return (0..6).map { monday.plusDays(it.toLong()).format(fmt) }
    }

    /**
     * 计算允许的最大周数
     * 一般学期为 20 周，这里允许手动切换到最多 25 周以覆盖考试周
     */
    const val MAX_WEEK = 25

    /** 最小周数 */
    const val MIN_WEEK = 1
}
