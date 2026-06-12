package com.example.simpleschedule.ui.screens.schedule

import com.example.simpleschedule.data.local.preferences.PreferencesManager
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.PeriodTime
import java.time.LocalDate

/**
 * 课程表主屏 UI 状态
 *
 * 单一数据源：所有 UI 展示所需数据集中于此 data class，
 * Composable 通过 collectAsState() 订阅此状态实现响应式更新。
 *
 * 设计原则：
 * - 不可变状态：所有字段 val，状态变更通过 ViewModel 的 copy() 完成
 * - 扁平化：避免嵌套状态对象，减少重组范围
 * - 完整自描述：无需额外查询即可渲染整个屏幕
 */
data class ScheduleUiState(
    /** 全部课程列表（未过滤） */
    val allCourses: List<Course> = emptyList(),

    /** 当前周过滤后的课程列表（仅包含本周应显示的课程） */
    val filteredCourses: List<Course> = emptyList(),

    /** 上一周过滤后的课程（滑动预览用） */
    val prevWeekCourses: List<Course> = emptyList(),

    /** 下一周过滤后的课程（滑动预览用） */
    val nextWeekCourses: List<Course> = emptyList(),

    /** 当前显示的教学周数 */
    val currentWeek: Int = 1,

    /** 手动周偏移量（0 = 自动本周，非0 = 已手动切换） */
    val weekOffset: Int = 0,

    /** 是否处于"自动本周"模式（true = 显示实际当前周，未手动偏移） */
    val isAutoWeek: Boolean = true,

    /** 学期开始日期（null 表示用户尚未设置） */
    val semesterStartDate: LocalDate? = null,

    /** 当前周周一日期（用于网格表头显示日期） */
    val currentWeekMonday: LocalDate? = null,

    /** 当前周每天日期标签（如 ["3/24","3/25",...]） */
    val dayDateLabels: List<String> = emptyList(),

    /** 当前周日期范围（如 "3/24-3/28"） */
    val weekDateRange: String = "",

    /** 是否首次启动（需引导用户设置开学日期） */
    val isFirstLaunch: Boolean = false,

    /** 是否显示课程编辑底部弹窗 */
    val showEditSheet: Boolean = false,

    /** 当前编辑的课程（null = 新建模式，非 null = 编辑模式） */
    val editingCourse: Course? = null,

    /** 数据是否正在加载 */
    val isLoading: Boolean = true,

    /** 节次时间表（12节课的上下课时间） */
    val periodTimes: List<PeriodTime> = PeriodTime.DEFAULTS,

    /** 卡片全局设置 */
    val cardSettings: PreferencesManager.CardSettings = PreferencesManager.CardSettings()
)
