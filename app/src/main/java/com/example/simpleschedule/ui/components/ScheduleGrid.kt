package com.example.simpleschedule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.PeriodTime
import com.example.simpleschedule.domain.model.timeFor
import kotlin.math.roundToInt

/**
 * 课程表网格组件（自适应行高版）
 *
 * 行高动态计算 = 可用高度 / 总节次数，始终充满屏幕。
 * 节次过多导致行高过小时，回退到可滚动模式（最低 40dp/行）。
 */
@Composable
fun ScheduleGrid(
    courses: List<Course>,
    periodTimes: List<PeriodTime> = PeriodTime.DEFAULTS,
    onCourseClick: (Course) -> Unit = {},
    modifier: Modifier = Modifier,
    dayDateLabels: List<String> = emptyList(),
    onCourseLongClick: ((Course) -> Unit)? = null,
    cardSettings: com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings =
        com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings()
) {
    val minRowHeight = 40.dp  // 最小行高，低于此值则启用滚动
    val periodLabelWidth = 44.dp
    val hasDates = dayDateLabels.size >= 7
    val headerHeight = if (hasDates) 48.dp else 32.dp
    // 动态节次数：默认 8 节，有更晚的课或自定义节次时自动扩展
    val maxPeriod = maxOf(
        courses.maxOfOrNull { it.endPeriod } ?: 1,
        8,
        periodTimes.size
    )

    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val density = LocalDensity.current

    Column(modifier = modifier) {
        // ---- 星期头行 ----
        Row(modifier = Modifier.fillMaxWidth().height(headerHeight)) {
            Box(modifier = Modifier.width(periodLabelWidth).fillMaxHeight())
            weekDays.forEachIndexed { index, day ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (hasDates && index < dayDateLabels.size) {
                            Text(
                                text = dayDateLabels[index],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ---- 网格主体：BoxWithConstraints 获取可用高度 ----
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // 动态计算行高：可用高度 / 节次数，不低于 minRowHeight
            val calculatedRowHeight = maxHeight / maxPeriod
            val needsScroll = calculatedRowHeight < minRowHeight
            val rowHeight = if (needsScroll) minRowHeight else calculatedRowHeight
            val totalHeight = rowHeight * maxPeriod

            val scrollState = if (needsScroll) rememberScrollState() else null

            val columnWidth: Dp = (maxWidth - periodLabelWidth) / 7

            // 像素值
            val periodLabelWidthPx = with(density) { periodLabelWidth.toPx() }
            val columnWidthPx = with(density) { columnWidth.toPx() }
            val rowHeightPx = with(density) { rowHeight.toPx() }
            // 直接使用 Dp 算术计算，避免 px↔dp 转换
            val gridWidth: Dp = periodLabelWidth + columnWidth * 7

            Box(
                modifier = Modifier
                    .width(gridWidth)
                    .let { mod ->
                        if (needsScroll) {
                            mod.height(maxHeight).verticalScroll(scrollState!!)
                        } else {
                            mod.height(totalHeight)
                        }
                    }
            ) {
                // ---- 层 1：网格背景线 ----
                val gridLineColor = MaterialTheme.colorScheme.outlineVariant
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    val lineWidth = 1f
                    for (period in 0..maxPeriod) {
                        val y = period * rowHeightPx
                        drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), lineWidth)
                    }
                    for (col in 0..7) {
                        val x = periodLabelWidthPx + col * columnWidthPx
                        drawLine(gridLineColor, Offset(x, 0f), Offset(x, rowHeightPx * maxPeriod), lineWidth)
                    }
                }

                // ---- 层 2：节次标签 ----
                for (period in 1..maxPeriod) {
                    val pt = periodTimes.timeFor(period)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = 0, y = ((period - 1) * rowHeightPx).roundToInt()) }
                            .size(width = periodLabelWidth, height = rowHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$period",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (pt != null) {
                                Text(
                                    text = pt.startTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight.times(0.85f)
                                )
                            }
                        }
                    }
                }

                // ---- 层 3：课程卡片 ----
                for (course in courses) {
                    val xOffset = periodLabelWidthPx + (course.dayOfWeek - 1) * columnWidthPx
                    val yOffset = (course.startPeriod - 1) * rowHeightPx
                    val cardWidth = columnWidth
                    val cardHeight = rowHeight * (course.endPeriod - course.startPeriod + 1).coerceAtLeast(1)

                    CourseCard(
                        course = course,
                        onClick = { onCourseClick(course) },
                        onLongClick = onCourseLongClick?.let { { it(course) } },
                        settings = cardSettings,
                        modifier = Modifier
                            .offset { IntOffset(x = xOffset.roundToInt(), y = yOffset.roundToInt()) }
                            .size(width = cardWidth, height = cardHeight)
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}
