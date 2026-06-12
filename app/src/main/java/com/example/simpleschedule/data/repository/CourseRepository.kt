package com.example.simpleschedule.data.repository

import com.example.simpleschedule.data.local.CourseStore
import com.example.simpleschedule.data.local.preferences.PreferencesManager
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.PeriodTime
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 课程数据仓库
 *
 * 统一数据访问层，封装 JSON 文件存储和 DataStore 偏好设置。
 * ViewModel 层仅与 Repository 交互，无需关心底层存储细节。
 *
 * 数据流：
 * CourseStore (MutableStateFlow) → Repository → ViewModel (StateFlow) → UI
 */
class CourseRepository(
    private val courseStore: CourseStore,
    private val prefs: PreferencesManager
) {

    // ===================== 课程数据操作 =====================

    /**
     * 课程列表响应式流
     * CourseStore 更新 → StateFlow 推送 → UI 自动刷新
     */
    val allCourses: Flow<List<Course>> = courseStore.courses

    /**
     * 初始化加载全部课程（应用启动时调用一次）
     */
    suspend fun loadCourses(): List<Course> = courseStore.loadAll()

    /**
     * 根据 ID 查询单条课程
     */
    fun getCourseById(id: Long): Course? =
        courseStore.courses.value.firstOrNull { it.id == id }

    /**
     * 保存课程：id == 0 时新增，否则更新
     */
    suspend fun saveCourse(course: Course): Long = courseStore.save(course)

    /**
     * 批量保存课程（一次写入，比逐个 save 更高效安全）
     */
    suspend fun saveAllCourses(courses: List<Course>) = courseStore.saveAll(courses)

    /**
     * 清空所有课程
     */
    suspend fun deleteAllCourses() = courseStore.deleteAll()

    /**
     * 删除课程
     */
    suspend fun deleteCourse(course: Course) = courseStore.delete(course)

    // ===================== 偏好设置操作 =====================

    /** 学期开始日期（响应式） */
    val semesterStartDate: Flow<LocalDate?> = prefs.semesterStartDate

    /** 手动周偏移量（响应式） */
    val weekOffset: Flow<Int> = prefs.weekOffset

    /** 持久化学期开始日期 */
    suspend fun setSemesterStartDate(date: LocalDate) {
        prefs.setSemesterStartDate(date)
    }

    /** 持久化手动周偏移量 */
    suspend fun setWeekOffset(offset: Int) {
        prefs.setWeekOffset(offset)
    }

    /** 节次时间表（响应式） */
    val periodTimes: Flow<List<PeriodTime>> = prefs.periodTimes

    /** 卡片全局设置（响应式） */
    val cardSettings: Flow<com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings> = prefs.cardSettings

    suspend fun setCardSettings(settings: com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings) = prefs.setCardSettings(settings)

    /** 持久化节次时间表 */
    suspend fun setPeriodTimes(times: List<PeriodTime>) {
        prefs.setPeriodTimes(times)
    }
}
