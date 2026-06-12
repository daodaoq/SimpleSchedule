package com.example.simpleschedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.WeekType
import com.example.simpleschedule.ui.theme.MorandiSage
import kotlinx.coroutines.launch

/**
 * 课程编辑底部弹窗（新建 / 编辑）
 *
 * 设计原则：
 * - 表单状态本地管理，仅在保存时提交到 ViewModel，避免频繁更新全局状态
 * - 必填项校验（课程名称 + 节次/周数合法性），通过按钮禁用 + 错误提示引导用户
 * - 使用 ModalBottomSheet，符合 Material 3 设计规范，适配单手操作
 * - 弹窗内容可垂直滚动，适配小屏设备和键盘弹出场景
 *
 * @param course 编辑中的课程（null = 新建模式）
 * @param sheetState 底部弹窗状态（控制展开/收起）
 * @param onDismiss 关闭弹窗回调
 * @param onSave 保存回调（传入构建好的课程对象）
 * @param onDelete 删除回调（仅编辑模式显示删除按钮）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditSheet(
    course: Course?,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    existingCourses: List<Course> = emptyList()
) {
    val isEditMode = course != null
    val scope = rememberCoroutineScope()

    // ===================== 本地表单状态 =====================
    // 使用 remember 管理，弹窗关闭重建时自动重置
    var courseName by remember(course) { mutableStateOf(course?.courseName ?: "") }
    var teacher by remember(course) { mutableStateOf(course?.teacher ?: "") }
    var classroom by remember(course) { mutableStateOf(course?.classroom ?: "") }
    var dayOfWeek by remember(course) { mutableIntStateOf(course?.dayOfWeek ?: 1) }
    var startPeriodStr by remember(course) { mutableStateOf((course?.startPeriod ?: 1).toString()) }
    var endPeriodStr by remember(course) { mutableStateOf((course?.endPeriod ?: 2).toString()) }
    var startWeekStr by remember(course) { mutableStateOf((course?.startWeek ?: 1).toString()) }
    var endWeekStr by remember(course) { mutableStateOf((course?.endWeek ?: 20).toString()) }
    var weekType by remember(course) { mutableStateOf(course?.weekType ?: WeekType.ALL) }
    var selectedColor by remember(course) { mutableStateOf(course?.color ?: MorandiSage) }

    // 表单校验错误信息
    var nameError by remember { mutableStateOf(false) }
    var periodError by remember { mutableStateOf(false) }
    var weekError by remember { mutableStateOf(false) }

    // 解析数字输入（容错处理）
    val startPeriod = startPeriodStr.toIntOrNull() ?: 1
    val endPeriod = endPeriodStr.toIntOrNull() ?: 1
    val startWeek = startWeekStr.toIntOrNull() ?: 1
    val endWeek = endWeekStr.toIntOrNull() ?: 1

    // 校验逻辑
    val isPeriodValid = startPeriod in 1..12 && endPeriod in 1..12 && startPeriod <= endPeriod
    val isWeekValid = startWeek in 1..25 && endWeek in 1..25 && startWeek <= endWeek
    val isNameValid = courseName.isNotBlank()

    // 时间冲突检测：同一天同一时段只能有一门课
    val hasConflict = existingCourses.any { c ->
        c.id != course?.id &&  // 排除自己（编辑模式）
        c.dayOfWeek == dayOfWeek &&
        c.startPeriod <= endPeriod && startPeriod <= c.endPeriod
    }
    val isFormValid = isNameValid && isPeriodValid && isWeekValid && !hasConflict

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 可滚动表单区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            // ---- 标题 ----
            Text(
                text = if (isEditMode) "编辑课程" else "新建课程",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 课程名称（必填） ----
            OutlinedTextField(
                value = courseName,
                onValueChange = {
                    courseName = it
                    nameError = false
                },
                label = { Text("课程名称 *") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("课程名称不能为空") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ---- 授课教师 ----
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text("授课教师") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ---- 上课教室 ----
            OutlinedTextField(
                value = classroom,
                onValueChange = { classroom = it },
                label = { Text("上课教室") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- 星期选择 ----
            Text(
                text = "上课星期",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
                dayLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = (dayOfWeek == index + 1),
                        onClick = { dayOfWeek = index + 1 },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- 上课节次 ----
            Text(
                text = "上课节次",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startPeriodStr,
                    onValueChange = {
                        startPeriodStr = it.filter { c -> c.isDigit() }.take(2)
                        periodError = false
                    },
                    label = { Text("开始节次") },
                    isError = periodError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endPeriodStr,
                    onValueChange = {
                        endPeriodStr = it.filter { c -> c.isDigit() }.take(2)
                        periodError = false
                    },
                    label = { Text("结束节次") },
                    isError = periodError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = if (periodError) {
                        { Text("节次无效") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            if (hasConflict) {
                Text(
                    "⚠ 该时段已有课程，请选择其他时间",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- 上课周数 ----
            Text(
                text = "上课周数",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startWeekStr,
                    onValueChange = {
                        startWeekStr = it.filter { c -> c.isDigit() }.take(2)
                        weekError = false
                    },
                    label = { Text("起始周") },
                    isError = weekError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endWeekStr,
                    onValueChange = {
                        endWeekStr = it.filter { c -> c.isDigit() }.take(2)
                        weekError = false
                    },
                    label = { Text("结束周") },
                    isError = weekError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = if (weekError) {
                        { Text("周数无效") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- 周类型 ----
            Text(
                text = "周类型",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WeekType.entries.forEach { wt ->
                    FilterChip(
                        selected = (weekType == wt),
                        onClick = { weekType = wt },
                        label = { Text(wt.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 卡片颜色 ----
            Text(
                text = "卡片颜色",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(8.dp))
            } // 结束可滚动表单

            // ---- 固定底部操作按钮 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditMode) {
                    OutlinedButton(
                        onClick = {
                            course?.let {
                                scope.launch { sheetState.hide() }
                                onDelete(it)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("删除") }
                }

                Button(
                    onClick = {
                        nameError = !isNameValid
                        periodError = !isPeriodValid
                        weekError = !isWeekValid
                        if (isFormValid) {
                            val savedCourse = Course(
                                id = course?.id ?: 0L,
                                courseName = courseName.trim(),
                                teacher = teacher.trim(),
                                classroom = classroom.trim(),
                                dayOfWeek = dayOfWeek,
                                startPeriod = startPeriod,
                                endPeriod = endPeriod,
                                startWeek = startWeek,
                                endWeek = endWeek,
                                weekType = weekType,
                                colorValue = selectedColor.toArgb().toLong()
                            )
                            scope.launch { sheetState.hide() }
                            onSave(savedCourse)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("保存") }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
