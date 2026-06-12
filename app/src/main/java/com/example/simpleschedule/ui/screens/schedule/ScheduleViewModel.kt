package com.example.simpleschedule.ui.screens.schedule

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.simpleschedule.SimpleScheduleApp
import com.example.simpleschedule.data.pdf.ParsedCourse
import com.example.simpleschedule.data.pdf.PdfCourseParser
import com.example.simpleschedule.data.repository.CourseRepository
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.PeriodTime
import com.example.simpleschedule.util.WeekCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 课程表核心 ViewModel
 *
 * 职责：
 * - 管理全部 UI 状态（ScheduleUiState）
 * - 处理用户交互（切换周、增删改课程）
 * - 组合数据流（课程列表 + 偏好设置 → 过滤后课程）
 * - 协调 Repository 层完成数据持久化
 *
 * 生命周期：跟随 Activity，设备旋转等配置变更时保持存活
 */
class ScheduleViewModel(
    private val repository: CourseRepository
) : ViewModel() {

    /** UI 状态的可变持有者，仅 ViewModel 内部可写 */
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // ===================== PDF 导入状态 =====================
    private val _pdfParsedCourses = MutableStateFlow<List<ParsedCourse>>(emptyList())
    val pdfParsedCourses: StateFlow<List<ParsedCourse>> = _pdfParsedCourses.asStateFlow()

    private val _pdfRawText = MutableStateFlow("")
    val pdfRawText: StateFlow<String> = _pdfRawText.asStateFlow()

    private val _pdfImportBusy = MutableStateFlow(false)
    val pdfImportBusy: StateFlow<Boolean> = _pdfImportBusy.asStateFlow()

    private val _showPdfImportSheet = MutableStateFlow(false)
    val showPdfImportSheet: StateFlow<Boolean> = _showPdfImportSheet.asStateFlow()

    init {
        // 启动时加载数据并计算当前周
        loadData()
    }

    // ===================== 数据加载 =====================

    /**
     * 初始化加载：
     * 1. 订阅课程数据流（自动响应增删改）
     * 2. 订阅偏好设置（开学日期 + 周偏移）
     * 3. 组合上述流，计算过滤后的课程
     */
    private fun loadData() {
        viewModelScope.launch {
            // 先从文件加载课程数据
            repository.loadCourses()

            // 组合数据流：课程列表 + 开学日期 + 周偏移 + 节次时间
            combine(
                repository.allCourses,
                repository.semesterStartDate,
                repository.weekOffset,
                repository.periodTimes,
                repository.cardSettings
            ) { courses, startDate, offset, times, cardSettings ->
                val currentWeek = calculateDisplayWeek(startDate, offset)
                val filtered = filterCoursesForWeek(courses, currentWeek)
                val prevFiltered = filterCoursesForWeek(courses, (currentWeek - 1).coerceAtLeast(1))
                val nextFiltered = filterCoursesForWeek(courses, (currentWeek + 1).coerceAtMost(25))

                _uiState.update { state ->
                    // 计算日期标签
                    val monday = if (startDate != null)
                        WeekCalculator.mondayOfWeek(startDate, currentWeek) else null
                    val dayDates = if (startDate != null)
                        WeekCalculator.dayDates(startDate, currentWeek) else emptyList()
                    val weekRange = if (startDate != null)
                        WeekCalculator.weekDateRange(startDate, currentWeek) else ""

                    state.copy(
                        allCourses = courses,
                        filteredCourses = filtered,
                        prevWeekCourses = prevFiltered,
                        nextWeekCourses = nextFiltered,
                        currentWeek = currentWeek,
                        weekOffset = offset,
                        semesterStartDate = startDate,
                        currentWeekMonday = monday,
                        dayDateLabels = dayDates,
                        weekDateRange = weekRange,
                        isAutoWeek = (offset == 0),
                        isFirstLaunch = (startDate == null),
                        cardSettings = cardSettings,
                        isLoading = false,
                        periodTimes = times
                    )
                }
            }.collect { /* 状态已通过 update 更新 */ }
        }
    }

    /**
     * 计算当前应显示的教学周
     * displayWeek = 自动计算的当前周 + 手动偏移
     */
    private fun calculateDisplayWeek(semesterStartDate: LocalDate?, offset: Int): Int {
        if (semesterStartDate == null) return 1
        val autoWeek = WeekCalculator.calculateCurrentWeek(semesterStartDate)
        val displayWeek = autoWeek + offset
        return displayWeek.coerceIn(WeekCalculator.MIN_WEEK, WeekCalculator.MAX_WEEK)
    }

    /**
     * 过滤课程：周数范围 + 单双周匹配
     */
    private fun filterCoursesForWeek(courses: List<Course>, week: Int): List<Course> =
        courses.filter { course ->
            // 1. 检查周数范围：课程起始周 ≤ 当前周 ≤ 课程结束周
            val inWeekRange = course.startWeek <= week && week <= course.endWeek
            // 2. 检查周类型：单双周或所有周
            val matchesType = WeekCalculator.matchesWeekType(week, course.weekType)
            inWeekRange && matchesType
        }

    // ===================== 周切换操作 =====================

    /** 切换到上一周（到第 1 周时不再减少 offset，避免累积） */
    fun previousWeek() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.currentWeek <= WeekCalculator.MIN_WEEK) return@launch
            repository.setWeekOffset(state.weekOffset - 1)
        }
    }

    /** 切换到下一周（到第 25 周时不再增加 offset） */
    fun nextWeek() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.currentWeek >= WeekCalculator.MAX_WEEK) return@launch
            repository.setWeekOffset(state.weekOffset + 1)
        }
    }

    /** 回到本周（清零偏移量） */
    fun resetToCurrentWeek() {
        viewModelScope.launch {
            repository.setWeekOffset(0)
        }
    }

    // ===================== 课程编辑操作 =====================

    /** 打开新建课程弹窗 */
    fun showAddSheet() {
        _uiState.update { it.copy(showEditSheet = true, editingCourse = null) }
    }

    /** 打开编辑课程弹窗 */
    fun showEditSheet(course: Course) {
        _uiState.update { it.copy(showEditSheet = true, editingCourse = course) }
    }

    /** 关闭编辑弹窗 */
    fun hideEditSheet() {
        _uiState.update { it.copy(showEditSheet = false, editingCourse = null) }
    }

    /** 保存课程（新建或更新） */
    fun saveCourse(course: Course) {
        viewModelScope.launch {
            repository.saveCourse(course)
            hideEditSheet()
        }
    }

    /** 删除课程 */
    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            hideEditSheet()
        }
    }

    // ===================== 偏好设置操作 =====================

    /** 保存节次时间表 */
    fun savePeriodTimes(times: List<PeriodTime>) {
        viewModelScope.launch {
            repository.setPeriodTimes(times)
        }
    }

    /** 设置学期开始日期（首次启动引导） */
    fun setSemesterStartDate(date: LocalDate) {
        viewModelScope.launch {
            repository.setSemesterStartDate(date)
            repository.setWeekOffset(0)
        }
    }

    // ===================== PDF 导入 =====================

    /** 触发 PDF 文件解析 */
    fun startPdfImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            _pdfImportBusy.value = true
            _showPdfImportSheet.value = true
            _pdfParsedCourses.value = emptyList()
            _pdfRawText.value = ""

            try {
                val times = _uiState.value.periodTimes
                val parseResult = PdfCourseParser.parseWithRaw(context, uri, times)
                val parsed = parseResult.first
                val rawText = parseResult.second
                _pdfParsedCourses.value = parsed
                // 诊断信息：原始文本 + 解析结果摘要
                val diag = buildString {
                    appendLine("=== PDF 提取文本（前2000字）===")
                    appendLine(rawText.take(2000))
                    appendLine()
                    appendLine("=== 解析结果（${parsed.size}门）===")
                    parsed.forEach { pc ->
                        val c = pc.course
                        appendLine("[${pc.confidence}] ${c.courseName} | 周${c.dayOfWeek} ${c.startPeriod}-${c.endPeriod}节 | ${c.startWeek}-${c.endWeek}周 | ${c.teacher} | ${c.classroom}")
                    }
                }
                _pdfRawText.value = diag
                android.util.Log.d("PdfParser", diag)
            } catch (e: Exception) {
                _pdfRawText.value = "解析失败：${e.message}"
            } finally {
                _pdfImportBusy.value = false
            }
        }
    }

    /** 确认导入选中的课程（覆盖旧数据） */
    fun importSelectedCourses(courses: List<Course>) {
        viewModelScope.launch {
            try {
                // 先清空所有旧课程
                repository.deleteAllCourses()
                // 过滤掉无效课程（dayOfWeek=0 表示时间待定的补充课程）
                val validCourses = courses.filter { it.dayOfWeek in 1..7 && it.startPeriod in 1..12 }
                if (validCourses.isNotEmpty()) {
                    repository.saveAllCourses(validCourses)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _showPdfImportSheet.value = false
            _pdfParsedCourses.value = emptyList()
        }
    }

    /** 保存卡片全局设置 */
    fun setCardSettings(settings: com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings) {
        viewModelScope.launch { repository.setCardSettings(settings) }
    }

    /** 清空所有课程 */
    fun clearAllCourses() {
        viewModelScope.launch {
            repository.deleteAllCourses()
        }
    }

    /** 关闭 PDF 导入弹窗 */
    fun dismissPdfImport() {
        _showPdfImportSheet.value = false
        _pdfParsedCourses.value = emptyList()
    }

    // ===================== Factory =====================

    companion object {
        /**
         * ViewModel Factory —— 通过 Application 获取 Repository
         * 使用 viewModelFactory DSL，无需手动实现 ViewModelProvider.Factory
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = SimpleScheduleApp.instance
                ScheduleViewModel(app.container.repository)
            }
        }
    }
}
