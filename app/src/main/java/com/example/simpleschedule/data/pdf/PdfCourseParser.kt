package com.example.simpleschedule.data.pdf

import android.content.Context
import android.net.Uri
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.PeriodTime
import com.example.simpleschedule.domain.model.WeekType
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Confidence { HIGH, MEDIUM, LOW }

data class ParsedCourse(
    val course: Course,
    val confidence: Confidence,
    val rawText: String = ""
)

/**
 * PDF 课程解析器 (v2)
 *
 * 新思路：PDF 课表和 HTML 课表的课程文本格式完全一致（11 行固定结构），
 * 直接复用相同的解析逻辑——按行匹配课程名/节次周次/教室/教师。
 */
object PdfCourseParser {

    /**
     * 解析 PDF 并同时返回原始文本（用于诊断展示）
     */
    suspend fun parseWithRaw(
        context: Context,
        uri: Uri,
        periodTimes: List<PeriodTime>
    ): Pair<List<ParsedCourse>, String> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)

        val text = extractText(context, uri)
        if (text.isBlank()) return@withContext emptyList<ParsedCourse>() to ""

        val courses = parseText(text)
        courses to text
    }

    suspend fun parse(
        context: Context,
        uri: Uri,
        periodTimes: List<PeriodTime>
    ): List<ParsedCourse> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)

        val text = extractText(context, uri)
        if (text.isBlank()) return@withContext emptyList()

        parseText(text)
    }

    private fun extractText(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { doc ->
                    PDFTextStripper().getText(doc)
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 核心：按课程文本块匹配
     *
     * 每门课的文本格式为 11 行：
     *   软件测试☆
     *   ①(1-2节)1-12周
     *   西校区 教3108(西)
     *   苏晶
     *   软件测试-0003
     *   软件2303;软件2304
     *   考试
     *   讲课:46,实验:8
     *   4
     *   48
     *   3.0
     *
     * 特征：第 1 行是课程名（含☆★标记），第 2 行含 "节" 和 "周"
     */
    private fun parseText(text: String): List<ParsedCourse> {
        val results = mutableListOf<ParsedCourse>()

        // 1. 按空行切分段落
        val cleaned = text
            .replace("\r\n", "\n").replace("\r", "\n")
            .replace(Regex("\n{3,}"), "\n\n")

        // 2. 分成行
        val allLines = cleaned.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 3. 找课程数据块：连续 3+ 行，包含课程特征
        var i = 0
        while (i < allLines.size) {
            val line = allLines[i]

            // 检测课程名行：含☆★等标记，且下一行包含"节"和"周"
            if (isCourseNameLine(line) && i + 2 < allLines.size) {
                val nextLine = allLines[i + 1]
                if (nextLine.contains("节") && nextLine.contains("周")) {
                    // 收集课程文本块（最多 12 行）
                    val block = mutableListOf<String>()
                    var j = i
                    while (j < allLines.size && block.size < 12) {
                        val l = allLines[j]
                        // 遇到下一门课的特征行就停止
                        if (block.size >= 3 && isCourseNameLine(l) &&
                            j + 1 < allLines.size && allLines[j + 1].contains("节") && allLines[j + 1].contains("周")
                        ) break
                        block.add(l)
                        j++
                    }

                    if (block.size >= 3) {
                        val course = parseBlock(block)
                        if (course != null) {
                            val confidence = if (course.teacher.isNotBlank() && course.classroom.isNotBlank()) {
                                Confidence.HIGH
                            } else Confidence.MEDIUM
                            results.add(ParsedCourse(course, confidence, block.joinToString(" | ")))
                        }
                    }
                    i = j
                    continue
                }
            }
            i++
        }

        // 4. 如果没有找到任何课程，尝试更宽松的匹配
        if (results.isEmpty()) {
            i = 0
            while (i < allLines.size) {
                // 宽松匹配：包含"节"和"周"的行
                if (allLines[i].contains("节") && allLines[i].contains("周") && i > 0) {
                    val block = mutableListOf<String>()
                    // 往前找课程名
                    if (i > 0) block.add(allLines[i - 1])
                    // 当前行和后续行
                    for (k in 0 until minOf(11, allLines.size - i)) {
                        block.add(allLines[i + k])
                    }
                    if (block.size >= 3) {
                        val course = parseBlock(block)
                        if (course != null) {
                            results.add(ParsedCourse(course, Confidence.LOW, block.joinToString(" | ")))
                        }
                    }
                }
                i++
            }
        }

        return results
    }

    /** 检查是否为课程名行：含☆★等标记 或 看起来像课程名 */
    private fun isCourseNameLine(line: String): Boolean {
        if (line.any { it in "☆★●◆♦" }) return true
        // 纯中文 2-20 字，不含时间数字和关键词
        if (line.length in 2..20 && line.all { it in '一'..'鿿' }) {
            if (line.none { it in "一二三四五六七八九十" }) return true
        }
        return false
    }

    /** 解析课程文本块，复用 HTML 解析的 11 行格式 */
    private fun parseBlock(lines: List<String>): Course? {
        val cleanLines = lines.filter { it.isNotBlank() }
        if (cleanLines.size < 3) return null

        // 找课程名：第一行，去掉标记
        val nameLine = cleanLines.firstOrNull {
            it.any { c -> c in "☆★●◆♦" } || (it.length in 2..20 && it.any { c -> c in '一'..'鿿' })
        } ?: return null
        val name = nameLine.replace(Regex("[☆★●◆♦]"), "").trim()
        if (name.isEmpty()) return null

        // 找周次/节次行
        val pwIdx = cleanLines.indexOfFirst {
            (it.contains("节") && it.contains("周")) || it.matches(Regex(".*\\(\\d+-\\d+节\\).*"))
        }
        if (pwIdx < 0) return null
        val pwLine = cleanLines[pwIdx]

        // 解析节次
        var startPeriod = 1
        var endPeriod = 2
        val pm = Regex("""\((\d+)-(\d+)节\)""").find(pwLine)
        if (pm != null) {
            startPeriod = pm.groupValues[1].toIntOrNull() ?: 1
            endPeriod = pm.groupValues[2].toIntOrNull() ?: 2
        }

        // 解析周次
        var startWeek = 1
        var endWeek = 20
        val ranges = Regex("""(\d+)-(\d+)周""").findAll(pwLine).toList()
        if (ranges.isNotEmpty()) {
            val first = ranges.first()
            startWeek = first.groupValues[1].toIntOrNull() ?: 1
            endWeek = first.groupValues[2].toIntOrNull() ?: 20
        } else {
            val single = Regex("""(\d+)周""").find(pwLine)
            if (single != null) {
                val w = single.groupValues[1].toIntOrNull() ?: 1
                startWeek = w; endWeek = w
            }
        }

        // 周类型
        val weekType = when {
            pwLine.contains("单") -> WeekType.ODD
            pwLine.contains("双") -> WeekType.EVEN
            else -> WeekType.ALL
        }

        // 教室：找含"校区"或"教"字的行
        var classroom = ""
        for (line in cleanLines) {
            if (line.contains("校区") || line.matches(Regex(".*教\\d+.*"))) {
                classroom = line.trim()
                break
            }
        }

        // 教师：在周次行之后找 2-4 字中文名
        var teacher = ""
        for (k in pwIdx + 1 until minOf(cleanLines.size, pwIdx + 5)) {
            val tl = cleanLines[k].trim()
            if (tl.length in 2..4 && tl.all { it in '一'..'鿿' } &&
                tl !in listOf("考试", "考查", "讲课", "实验") &&
                !tl.contains("软件") && !tl.contains("移动") && !tl.contains("形势")
            ) {
                teacher = tl
                break
            }
        }

        // 星期：从 PDF 文本中难以准确提取，默认周一
        val dayOfWeek = guessDayOfWeek(cleanLines)

        return Course(
            courseName = name,
            teacher = teacher,
            classroom = classroom,
            dayOfWeek = dayOfWeek.coerceIn(1, 7),
            startPeriod = startPeriod.coerceIn(1, 12),
            endPeriod = endPeriod.coerceIn(1, 12),
            startWeek = startWeek.coerceIn(1, 25),
            endWeek = endWeek.coerceIn(1, 25),
            weekType = weekType
        )
    }

    /** 从文本中推测星期 */
    private fun guessDayOfWeek(lines: List<String>): Int {
        val joined = lines.joinToString(" ")
        // 检查是否包含星期信息
        val dayMap = mapOf(
            "周一" to 1, "星期一" to 1,
            "周二" to 2, "星期二" to 2,
            "周三" to 3, "星期三" to 3,
            "周四" to 4, "星期四" to 4,
            "周五" to 5, "星期五" to 5,
            "周六" to 6, "星期六" to 6,
            "周日" to 7, "星期日" to 7
        )
        for ((key, day) in dayMap) {
            if (joined.contains(key)) return day
        }
        return 1 // 无法确定时默认周一
    }
}
