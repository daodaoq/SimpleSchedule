package com.example.simpleschedule.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 可滑动翻页的周课表容器（双页跟手联动）
 *
 * 手势效果：
 * - 左滑：当前页向左退场，下一页从右侧跟入
 * - 右滑：当前页向右退场，上一页从左侧跟入
 * - 两页始终连贯，位移完全跟随手指
 *
 * @param currentWeek      当前周数
 * @param minWeek          最小周数（边界）
 * @param maxWeek          最大周数（边界）
 * @param onPreviousWeek   翻到上一周
 * @param onNextWeek       翻到下一周
 * @param previousContent  上一周的内容（用于预渲染）
 * @param currentContent   当前周的内容
 * @param nextContent      下一周的内容（用于预渲染）
 */
@Composable
fun SwipeableWeekPager(
    currentWeek: Int,
    minWeek: Int,
    maxWeek: Int,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier,
    previousContent: @Composable () -> Unit = {},
    currentContent: @Composable () -> Unit,
    nextContent: @Composable () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 手指拖动的实时偏移量（px）
    val dragOffset = remember { Animatable(0f) }
    // 容器宽度（px），用于计算页面滑入比例
    var containerWidth by remember { mutableFloatStateOf(1f) }
    // 是否在拖动中（用于决定是否显示相邻页）
    var isDragging by remember { mutableStateOf(false) }
    // 拖动方向：<0 左滑（看下一页），>0 右滑（看上一页）
    var dragDirection by remember { mutableFloatStateOf(0f) }

    val atStart = currentWeek <= minWeek
    val atEnd = currentWeek >= maxWeek

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerWidth = it.width.toFloat() }
            .pointerInput(currentWeek) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragDirection = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            val offset = dragOffset.value
                            val threshold = containerWidth * 0.3f

                            when {
                                // 左滑过阈值 → 翻到下一页
                                offset < -threshold && !atEnd -> {
                                    onNextWeek()
                                    dragOffset.snapTo(0f)
                                }
                                // 右滑过阈值 → 翻到上一页
                                offset > threshold && !atStart -> {
                                    onPreviousWeek()
                                    dragOffset.snapTo(0f)
                                }
                                // 边界或未达阈值 → 回弹
                                else -> {
                                    dragOffset.animateTo(0f, spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ))
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            dragOffset.animateTo(0f, spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ))
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            var newOffset = dragOffset.value + dragAmount

                            // 边界阻尼
                            if (atStart && newOffset > 0f) newOffset *= 0.25f
                            if (atEnd && newOffset < 0f) newOffset *= 0.25f

                            // 限制最大拖拽距离不超过容器宽度
                            newOffset = newOffset.coerceIn(-containerWidth, containerWidth)

                            dragOffset.snapTo(newOffset)
                            dragDirection = newOffset
                        }
                    }
                )
            }
    ) {
        // 计算各页的水平位移
        val currentOffset = dragOffset.value
        // 进度：0=中心，1=完全移出屏幕
        val progress = (abs(currentOffset) / containerWidth).coerceIn(0f, 1f)

        // ---- 上一页（从左侧滑入，仅右滑时可见）----
        if (currentOffset > 0f && !atStart) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 从左侧移入：起点 -containerWidth，终点 0
                        translationX = -containerWidth + currentOffset
                        alpha = progress.coerceIn(0f, 1f)
                    }
            ) {
                previousContent()
            }
        }

        // ---- 当前页（居中，向拖动方向移出）----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = currentOffset
                    alpha = 1f - progress * 0.4f
                }
        ) {
            currentContent()
        }

        // ---- 下一页（从右侧滑入，仅左滑时可见）----
        if (currentOffset < 0f && !atEnd) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 从右侧移入：起点 containerWidth，终点 0
                        translationX = containerWidth + currentOffset
                        alpha = progress.coerceIn(0f, 1f)
                    }
            ) {
                nextContent()
            }
        }
    }
}
