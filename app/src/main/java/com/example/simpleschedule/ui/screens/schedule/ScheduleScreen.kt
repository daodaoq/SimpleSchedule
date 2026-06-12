package com.example.simpleschedule.ui.screens.schedule

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.ui.components.CourseEditSheet
import com.example.simpleschedule.ui.components.PdfImportSheet
import com.example.simpleschedule.ui.components.PeriodTimeSettingsSheet
import com.example.simpleschedule.ui.components.ScheduleGrid
import com.example.simpleschedule.ui.components.SwipeableWeekPager
import com.example.simpleschedule.ui.components.WeekSelector
import com.example.simpleschedule.util.WeekCalculator
import com.example.simpleschedule.ui.screens.webimport.SchoolSelectScreen
import com.example.simpleschedule.ui.screens.webimport.SdutWebImportScreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 课程表主屏幕
 *
 * 职责：
 * 1. 组装所有子组件（WeekSelector + ScheduleGrid + FAB + CourseEditSheet）
 * 2. 处理首次启动的开学日期设置流程
 * 3. 将用户交互转发给 ViewModel
 *
 * 架构：此 Composable 是无状态的——所有状态来自 ScheduleViewModel.uiState
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val pdfParsed by viewModel.pdfParsedCourses.collectAsState()
    val pdfRawText by viewModel.pdfRawText.collectAsState()
    val pdfBusy by viewModel.pdfImportBusy.collectAsState()
    val showPdfSheet by viewModel.showPdfImportSheet.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // WebView 导入状态
    var showSchoolSelect by remember { mutableStateOf(false) }
    var showWebImport by remember { mutableStateOf(false) }
    var selectedSchool by remember { mutableStateOf<com.example.simpleschedule.ui.screens.webimport.SchoolInfo?>(null) }

    // PDF 文件选择器
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.startPdfImport(context, it) }
    }

    // 控制开学日期选择弹窗的显示
    var showDatePicker by remember { mutableStateOf(false) }
    // 控制上课时间设置弹窗的显示
    var showTimeSettings by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showCardSettings by remember { mutableStateOf(false) }
    var previewCourse by remember { mutableStateOf<Course?>(null) }
    val timeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ===================== 首次启动：引导设置开学日期 =====================
    if (uiState.isFirstLaunch || showDatePicker) {
        SemesterStartDatePicker(
            onDateSelected = { selectedDate ->
                viewModel.setSemesterStartDate(selectedDate)
                showDatePicker = false
            },
            onDismiss = {
                showDatePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    WeekSelector(
                        currentWeek = uiState.currentWeek,
                        isAutoWeek = uiState.isAutoWeek,
                        onPrevious = { viewModel.previousWeek() },
                        onNext = { viewModel.nextWeek() },
                        onReset = { viewModel.resetToCurrentWeek() },
                        weekDateRange = uiState.weekDateRange
                    )
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("导入课表") },
                                onClick = { menuExpanded = false; showSchoolSelect = true }
                            )
                            DropdownMenuItem(
                                text = { Text("上课时间") },
                                onClick = { menuExpanded = false; showTimeSettings = true }
                            )
                            DropdownMenuItem(
                                text = { Text("学期日期") },
                                onClick = { menuExpanded = false; showDatePicker = true }
                            )
                            DropdownMenuItem(
                                text = { Text("清空课表", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; showClearConfirm = true }
                            )
                            DropdownMenuItem(
                                text = { Text("卡片设置") },
                                onClick = { menuExpanded = false; showCardSettings = true }
                            )
                            DropdownMenuItem(
                                text = { Text("关于") },
                                onClick = { menuExpanded = false; showAbout = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // 新建课程按钮
            FloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加课程"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // 数据加载中
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // 已加载但无课程 + 非首次启动
                uiState.allCourses.isEmpty() && !uiState.isFirstLaunch -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📚",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize.times(3)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "还没有课程",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showSchoolSelect = true },
                                modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("📋 导入课表", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "或点击右下角 + 手动添加",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 正常显示课程表（支持左右滑动翻页）
                else -> {
                    SwipeableWeekPager(
                        currentWeek = uiState.currentWeek,
                        minWeek = WeekCalculator.MIN_WEEK,
                        maxWeek = WeekCalculator.MAX_WEEK,
                        onPreviousWeek = { viewModel.previousWeek() },
                        onNextWeek = { viewModel.nextWeek() },
                        previousContent = {
                            ScheduleGrid(
                                courses = uiState.prevWeekCourses,
                                periodTimes = uiState.periodTimes,
                                onCourseClick = { },
                                modifier = Modifier.fillMaxWidth(),
                                cardSettings = uiState.cardSettings
                            )
                        },
                        currentContent = {
                            ScheduleGrid(
                                courses = uiState.filteredCourses,
                                periodTimes = uiState.periodTimes,
                                onCourseClick = { course -> previewCourse = course },
                                onCourseLongClick = { course -> viewModel.showEditSheet(course) },
                                modifier = Modifier.fillMaxWidth(),
                                dayDateLabels = uiState.dayDateLabels,
                                cardSettings = uiState.cardSettings
                            )
                        },
                        nextContent = {
                            ScheduleGrid(
                                courses = uiState.nextWeekCourses,
                                periodTimes = uiState.periodTimes,
                                onCourseClick = { },
                                modifier = Modifier.fillMaxWidth(),
                                cardSettings = uiState.cardSettings
                            )
                        }
                    )
                }
            }
        }
    }

    // ===================== 课程编辑弹窗 =====================
    if (uiState.showEditSheet) {
        CourseEditSheet(
            course = uiState.editingCourse,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }
                viewModel.hideEditSheet()
            },
            onSave = { course -> viewModel.saveCourse(course) },
            onDelete = { course -> viewModel.deleteCourse(course) },
            existingCourses = uiState.allCourses
        )
    }

    // ===================== 上课时间设置弹窗 =====================
    if (showTimeSettings) {
        PeriodTimeSettingsSheet(
            currentTimes = uiState.periodTimes,
            sheetState = timeSheetState,
            onDismiss = {
                scope.launch { timeSheetState.hide() }
                showTimeSettings = false
            },
            onSave = { times ->
                viewModel.savePeriodTimes(times)
                showTimeSettings = false
            }
        )
    }

    // ===================== PDF 导入弹窗 =====================
    if (showPdfSheet) {
        val pdfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        PdfImportSheet(
            parsedCourses = pdfParsed,
            rawText = pdfRawText,
            isParsing = pdfBusy,
            sheetState = pdfSheetState,
            onDismiss = {
                scope.launch { pdfSheetState.hide() }
                viewModel.dismissPdfImport()
            },
            onImport = { courses ->
                viewModel.importSelectedCourses(courses)
            }
        )
    }

    // ===================== 课程预览 =====================
    previewCourse?.let { c ->
        AlertDialog(
            onDismissRequest = { previewCourse = null },
            title = { Text(c.courseName, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row { Text("教师：", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text(c.teacher.ifBlank { "未指定" }) }
                    Row { Text("教室：", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text(c.classroom.ifBlank { "未指定" }) }
                    Row { Text("时间：", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text("周${c.dayOfWeek} ${c.startPeriod}-${c.endPeriod}节") }
                    Row {
                        Text("周次：", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp))
                        Text(if (c.startWeek == c.endWeek) "第${c.startWeek}周" else "${c.startWeek}-${c.endWeek}周${if (c.weekType != com.example.simpleschedule.domain.model.WeekType.ALL) " (${c.weekType.label})" else ""}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("💡 长按卡片可直接编辑", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val course = c
                    previewCourse = null
                    viewModel.showEditSheet(course)
                }) { Text("编辑") }
            },
            dismissButton = {
                TextButton(onClick = { previewCourse = null }) { Text("关闭") }
            }
        )
    }

    // ===================== 清空课表确认 =====================
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空课表") },
            text = { Text("确定要清空所有课程吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAllCourses()
                }) { Text("确定清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }

    // ===================== 卡片设置 =====================
    if (showCardSettings) {
        val settings = uiState.cardSettings
        var opacity by remember(settings) { mutableFloatStateOf(settings.opacity) }
        var fontScale by remember(settings) { mutableFloatStateOf(settings.fontScale) }
        var cornerRadius by remember(settings) { mutableFloatStateOf(settings.cornerRadius.toFloat()) }
        var showTeacher by remember(settings) { mutableStateOf(settings.showTeacher) }
        var showClassroom by remember(settings) { mutableStateOf(settings.showClassroom) }

        AlertDialog(
            onDismissRequest = { showCardSettings = false },
            title = { Text("卡片通用设置") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("透明度: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.3f..1f)
                    Spacer(Modifier.height(8.dp))
                    Text("字体大小: ${(fontScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(value = fontScale, onValueChange = { fontScale = it }, valueRange = 0.8f..1.3f)
                    Spacer(Modifier.height(8.dp))
                    Text("圆角大小: ${cornerRadius.toInt()}dp", style = MaterialTheme.typography.bodySmall)
                    Slider(value = cornerRadius, onValueChange = { cornerRadius = it }, valueRange = 4f..12f, steps = 7)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("显示教师")
                        Switch(checked = showTeacher, onCheckedChange = { showTeacher = it })
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("显示教室")
                        Switch(checked = showClassroom, onCheckedChange = { showClassroom = it })
                    }
                }
            },
            confirmButton = {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        opacity = 1f; fontScale = 1f; cornerRadius = 8f
                        showTeacher = true; showClassroom = true
                    }) { Text("恢复默认", color = MaterialTheme.colorScheme.error) }
                    Row {
                        TextButton(onClick = { showCardSettings = false }) { Text("取消") }
                        TextButton(onClick = {
                            showCardSettings = false
                            viewModel.setCardSettings(
                                com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings(
                            opacity = opacity,
                            fontScale = fontScale,
                            cornerRadius = cornerRadius.toInt(),
                            showTeacher = showTeacher,
                            showClassroom = showClassroom
                        )
                        )
                    }) { Text("应用") }
                }
            }
            }
        )
    }

    // ===================== 关于 =====================
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            icon = { Text("🎓", style = MaterialTheme.typography.headlineMedium) },
            title = { Text("课程通") },
            text = {
                Column {
                    Text("版本 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("作者：sjk", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("反馈QQ：2090953265", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            val clip = android.content.ClipData.newPlainText("qq", "2090953265")
                            (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "QQ号已复制", android.widget.Toast.LENGTH_SHORT).show()
                        }) { Text("复制", fontSize = MaterialTheme.typography.labelSmall.fontSize) }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("开源仓库：github.com/daodaoq/SimpleSchedule", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = {
                        val clip = android.content.ClipData.newPlainText("url", "https://github.com/daodaoq/SimpleSchedule")
                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "链接已复制", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text("📋 复制仓库链接") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("关闭") }
            }
        )
    }

    // ===================== 学校选择（导入方式选择） =====================
    if (showSchoolSelect) {
        SchoolSelectScreen(
            onBack = { showSchoolSelect = false },
            onSchoolSelected = { school ->
                selectedSchool = school
                showSchoolSelect = false
                showWebImport = true
            },
            onPdfImport = {
                showSchoolSelect = false
                pdfLauncher.launch(arrayOf("application/pdf"))
            }
        )
    }

    // ===================== WebView 官网导入 =====================
    if (showWebImport && selectedSchool != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            SdutWebImportScreen(
                school = selectedSchool!!,
                onBack = { showWebImport = false; showSchoolSelect = true },
                onCoursesParsed = { courses ->
                    showWebImport = false
                    viewModel.importSelectedCourses(courses)
                }
            )
        }
    }
}

/**
 * 开学日期选择弹窗 — 年月日三个下拉按钮，快速选日期。
 */
@Composable
private fun SemesterStartDatePicker(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    var year by remember { mutableStateOf(today.year) }
    var month by remember { mutableStateOf(today.monthValue) }
    var day by remember { mutableStateOf(1) }
    var picking by remember { mutableStateOf("") }

    val daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth()
    val years = (2020..2030).toList()
    val months = (1..12).toList()
    val days = (1..if (day > daysInMonth) { day = daysInMonth; daysInMonth } else daysInMonth).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("请选择第一周周一日期") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 年
                    Box {
                        TextButton(onClick = { picking = "year" }) {
                            Text("${year}年", style = MaterialTheme.typography.titleMedium)
                        }
                        DropdownMenu(expanded = picking == "year", onDismissRequest = { picking = "" }) {
                            years.forEach { y ->
                                DropdownMenuItem(text = { Text("${y}年") }, onClick = { year = y; picking = "" })
                            }
                        }
                    }
                    // 月
                    Box {
                        TextButton(onClick = { picking = "month" }) {
                            Text("${month}月", style = MaterialTheme.typography.titleMedium)
                        }
                        DropdownMenu(expanded = picking == "month", onDismissRequest = { picking = "" }) {
                            months.forEach { m ->
                                DropdownMenuItem(text = { Text("${m}月") }, onClick = { month = m; picking = "" })
                            }
                        }
                    }
                    // 日
                    Box {
                        TextButton(onClick = { picking = "day" }) {
                            Text("${day}日", style = MaterialTheme.typography.titleMedium)
                        }
                        DropdownMenu(expanded = picking == "day", onDismissRequest = { picking = "" }) {
                            days.forEach { d ->
                                DropdownMenuItem(text = { Text("${d}日") }, onClick = { day = d; picking = "" })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDateSelected(LocalDate.of(year, month, day)) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
