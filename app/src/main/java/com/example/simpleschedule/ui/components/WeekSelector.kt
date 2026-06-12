package com.example.simpleschedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 顶部周选择器组件
 *
 * 提供教学周的左右切换、当前周数显示、日期范围、"回到本周"快速复位功能。
 *
 * 布局：[◀] [第 N 周] [▶] [回到本周]
 *              [日期范围]
 */
@Composable
fun WeekSelector(
    currentWeek: Int,
    isAutoWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    weekDateRange: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左箭头：上一周
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上一周"
            )
        }

        // 中央：周数 + 日期范围
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isAutoWeek) "本周 · 第 $currentWeek 周" else "第 $currentWeek 周",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isAutoWeek) FontWeight.Bold else FontWeight.Medium,
                color = if (isAutoWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (weekDateRange.isNotEmpty()) {
                Text(
                    text = weekDateRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 右箭头：下一周
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一周"
            )
        }

        // 仅在手动偏移时显示"回到本周"按钮
        if (!isAutoWeek) {
            TextButton(onClick = onReset) {
                Text(
                    text = "回到本周",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else {
            // 占位，保持布局稳定
            TextButton(
                onClick = {},
                enabled = false
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
