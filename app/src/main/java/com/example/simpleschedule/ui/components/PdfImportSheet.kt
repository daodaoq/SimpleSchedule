package com.example.simpleschedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.data.pdf.Confidence
import com.example.simpleschedule.data.pdf.ParsedCourse
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.WeekType
import kotlinx.coroutines.launch

/**
 * PDF 导入预览弹窗
 *
 * 展示解析结果，用户勾选确认后批量导入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfImportSheet(
    parsedCourses: List<ParsedCourse>,
    rawText: String = "",
    isParsing: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onImport: (List<Course>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val checkedIndices = remember { mutableStateListOf<Int>() }

    // 默认勾选所有 HIGH 和 MEDIUM 置信度的课程
    LaunchedEffect(parsedCourses) {
        checkedIndices.clear()
        parsedCourses.forEachIndexed { i, pc ->
            if (pc.confidence != Confidence.LOW) checkedIndices.add(i)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // ---- 标题 ----
            Text("PDF 课程导入", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(12.dp))

            when {
                // 解析中
                isParsing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("正在解析 PDF…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // 未识别到课程
                parsedCourses.isEmpty() -> {
                    Text(
                        "未能自动识别课程",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "请确认 PDF 文件是否包含课程表（非图片格式）。以下为提取到的原始文本，可供手动录入参考：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(200.dp).verticalScroll(rememberScrollState()),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = rawText.ifBlank { "（未提取到文字内容）" },
                            modifier = Modifier.padding(12.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("关闭")
                    }
                }

                // 展示课程列表
                else -> {
                    val highCount = parsedCourses.count { it.confidence == Confidence.HIGH }
                    val mediumCount = parsedCourses.count { it.confidence == Confidence.MEDIUM }
                    val lowCount = parsedCourses.count { it.confidence == Confidence.LOW }

                    // 统计行
                    Text(
                        text = "识别到 ${parsedCourses.size} 门课程（高:$highCount 中:$mediumCount 低:$lowCount）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已勾选 ${checkedIndices.size} 门待导入",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 课程列表
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(parsedCourses) { index, parsed ->
                            val isChecked = index in checkedIndices
                            ImportCourseItem(
                                parsed = parsed,
                                isChecked = isChecked,
                                onToggle = {
                                    if (isChecked) checkedIndices.remove(index)
                                    else checkedIndices.add(index)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 底部操作栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 全选/取消
                        OutlinedButton(
                            onClick = {
                                if (checkedIndices.size == parsedCourses.size) {
                                    checkedIndices.clear()
                                } else {
                                    checkedIndices.clear()
                                    parsedCourses.indices.forEach { checkedIndices.add(it) }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (checkedIndices.size == parsedCourses.size) "取消全选" else "全选")
                        }

                        // 导入已选
                        Button(
                            onClick = {
                                val selected = checkedIndices.map { parsedCourses[it].course }
                                scope.launch { sheetState.hide() }
                                onImport(selected)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = checkedIndices.isNotEmpty()
                        ) {
                            Text("导入已选（${checkedIndices.size}）")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 单条导入课程项
 */
@Composable
private fun ImportCourseItem(
    parsed: ParsedCourse,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val c = parsed.course
    val conf = parsed.confidence

    val confColor = when (conf) {
        Confidence.HIGH -> Color(0xFF4CAF50)
        Confidence.MEDIUM -> Color(0xFFFFA726)
        Confidence.LOW -> Color(0xFFEF5350)
    }
    val confLabel = when (conf) {
        Confidence.HIGH -> "高"
        Confidence.MEDIUM -> "中"
        Confidence.LOW -> "低"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(8.dp),
        color = c.color.copy(alpha = 0.15f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 复选框
            Checkbox(checked = isChecked, onCheckedChange = { onToggle() })

            // 课程信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = c.courseName.ifBlank { "（未识别）" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // 置信度标签
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = confColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = confLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = confColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val info = buildString {
                    val dayNames = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
                    append(dayNames.getOrElse(c.dayOfWeek) { "" })
                    append(" ${c.startPeriod}-${c.endPeriod}节")
                    if (c.classroom.isNotBlank()) append(" · ${c.classroom}")
                    if (c.teacher.isNotBlank()) append(" · ${c.teacher}")
                    append(" · ${c.startWeek}-${c.endWeek}周")
                    if (c.weekType != WeekType.ALL) append(" · ${c.weekType.label}")
                }
                Text(
                    text = info.trim(),
                    fontSize = 12.sp,
                    color = Color(0xFF555555),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
