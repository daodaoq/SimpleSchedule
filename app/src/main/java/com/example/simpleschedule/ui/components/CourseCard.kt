package com.example.simpleschedule.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.WeekType

/**
 * 课程卡片组件
 *
 * 信息层级清晰：课程名 → 教师/教室（带前缀标签）→ 周数 + 周类型
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseCard(
    course: Course,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    settings: com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings =
        com.example.simpleschedule.data.local.preferences.PreferencesManager.CardSettings()
) {
    val backgroundColor = course.color.copy(alpha = settings.opacity)
    val fs = 12 * settings.fontScale
    val fsSmall = 9 * settings.fontScale
    val lh = 15 * settings.fontScale
    val lhSmall = 12 * settings.fontScale

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(settings.cornerRadius.dp))
            .let { m ->
                if (onLongClick != null) m.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                else m.clickable { onClick() }
            },
        shape = RoundedCornerShape(settings.cornerRadius.dp),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---- 课程名称（粗体，最醒目）----
            Text(
                text = course.courseName,
                fontWeight = FontWeight.Bold,
                fontSize = fs.sp,
                color = Color(0xFF2C2C2C),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = lh.sp
            )

            // ---- 教师 ----
            if (settings.showTeacher && course.teacher.isNotBlank()) {
                Text(
                    text = "教师：${course.teacher}",
                    fontSize = fsSmall.sp,
                    color = Color(0xFF555555),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = lhSmall.sp
                )
            }

            // ---- 教室 ----
            if (settings.showClassroom && course.classroom.isNotBlank()) {
                Text(
                    text = course.classroom,
                    fontSize = fsSmall.sp,
                    color = Color(0xFF555555),
                    softWrap = true,
                    lineHeight = lhSmall.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ---- 底部：周数范围 + 单双周标签 ----
            val weekText = if (course.startWeek == course.endWeek) {
                "第${course.startWeek}周"
            } else {
                "${course.startWeek}-${course.endWeek}周"
            }

            Text(
                text = weekText,
                fontSize = fsSmall.sp,
                color = Color(0xFF4A4A4A),
                maxLines = 2,
                fontWeight = FontWeight.Medium,
                lineHeight = lhSmall.sp
            )

            if (course.weekType != WeekType.ALL) {
                WeekTypeBadge(weekType = course.weekType)
            }
        }
    }
}

/**
 * 单/双周小标签
 */
@Composable
private fun WeekTypeBadge(weekType: WeekType) {
    val label = when (weekType) {
        WeekType.ODD  -> "单周"
        WeekType.EVEN -> "双周"
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color.Black.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A4A4A),
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
            lineHeight = 10.sp
        )
    }
}
