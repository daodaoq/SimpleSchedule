package com.example.simpleschedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.simpleschedule.ui.theme.MorandiColorPalette

/**
 * 莫兰迪色系颜色选择器
 *
 * 以圆形色块网格展示 12 种课程卡片颜色供用户选择。
 * 选中状态以白色勾号 + 外边框高亮指示。
 *
 * 交互：点击色块即可选中，视觉反馈即时。
 *
 * @param selectedColor 当前选中的颜色
 * @param onColorSelected 颜色被点击时的回调
 * @param modifier 外部修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MorandiColorPalette.forEach { color ->
            ColorCircle(
                color = color,
                isSelected = (color == selectedColor),
                onClick = { onColorSelected(color) }
            )
        }
    }
}

/**
 * 单个可选颜色圆圈
 *
 * 选中态显示中心白色对勾 + 外圈深色描边，确保在浅色色块上也清晰可见。
 */
@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color, CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 选中状态：显示白色对勾
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选中",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
