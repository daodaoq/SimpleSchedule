package com.example.simpleschedule.ui.screens.webimport

import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.WeekType
import org.json.JSONArray
import org.json.JSONObject

/**
 * 山东理工大学教务系统课表解析器
 *
 * 提供两件事：
 * 1. JavaScript 注入脚本 —— 在 WebView 中执行，从 HTML DOM 提取课程 JSON
 * 2. JSON → List<Course> 映射函数
 *
 * 解析目标：
 * - 表头列：周一~周日（第 3~9 列）
 * - 表体行：节次 1~10，每行单元格含多行结构化文本
 * - 单元格文本行依次为：课程名、节次/周次、教室、教师、...
 */
object SdutTableParser {

    /**
     * 注入 WebView 的 JavaScript 解析脚本
     *
     * 逻辑：
     * 1. 找到课程表 table（id 含 "kbtable" 或 class 含 "table"）
     * 2. 遍历 tr → 读取第二列(节次) → 遍历第3~9列(星期)
     * 3. 对有内容的单元格，按换行拆分文本
     * 4. 提取：课程名、节次范围、周次范围、教室、教师
     * 5. 组装 JSON 数组返回
     */
    val JS_PARSE_SCRIPT = """
(function() {
    // 辅助函数：从 innerHTML 提取多行文本
    function htmlToLines(html) {
        if (!html) return [];
        var t = html.replace(/<br[^>]*>/gi, '\n');
        t = t.replace(/<\/[^>]+>/gi, '\n');
        t = t.replace(/<[^>]*>/g, '');
        var ta = document.createElement('textarea');
        ta.innerHTML = t;
        t = ta.value || t;
        var rawLines = t.split('\n');
        var clean = [];
        for (var i = 0; i < rawLines.length; i++) {
            var line = rawLines[i].trim();
            if (line) clean.push(line);
        }
        return clean;
    }

    // 颜色调色板（ARGB，带微透明感），同课程名 → 同颜色
    var COLOR_PALETTE = [
        0xDD90CAF9, // 浅蓝
        0xDDFFCC80, // 橙色
        0xDDA5D6A7, // 浅绿
        0xDDEF9A9A, // 浅红
        0xDD80CBC4, // 青绿
        0xDDCE93D8, // 紫色
        0xDDFFF59D, // 黄色
        0xDDBCAAA4, // 棕色
        0xDD81D4FA, // 天蓝
        0xDDFFAB91, // 鲑鱼色
        0xDDC5E1A5, // 黄绿
        0xDDF48FB1, // 粉色
        0xDDB0BEC5, // 蓝灰
        0xDDFFE082, // 金色
        0xDD9FA8DA, // 靛蓝
        0xDDA3B5A6  // 灰绿（原默认色）
    ];

    function getColor(courseName) {
        var hash = 0;
        for (var i = 0; i < courseName.length; i++) {
            hash = ((hash << 5) - hash) + courseName.charCodeAt(i);
            hash |= 0;
        }
        return COLOR_PALETTE[Math.abs(hash) % COLOR_PALETTE.length];
    }

    try {
        var diagLines = [];

        // ========== 1. 查找课表表格 ==========
        var tables = document.querySelectorAll('table');
        var targetTable = null;
        for (var i = 0; i < tables.length; i++) {
            var txt = tables[i].innerText || '';
            if (txt.indexOf('时间段') >= 0 && txt.indexOf('节次') >= 0 &&
                txt.indexOf('星期一') >= 0) {
                targetTable = tables[i];
                break;
            }
        }
        if (!targetTable) {
            var maxRows = 0;
            for (var i = 0; i < tables.length; i++) {
                var n = tables[i].querySelectorAll('tr').length;
                if (n > maxRows) { maxRows = n; targetTable = tables[i]; }
            }
        }
        if (!targetTable) {
            return JSON.stringify({ error: '未找到课表表格' });
        }

        var allRows = targetTable.querySelectorAll('tr');
        diagLines.push('T' + tables.length + 'R' + allRows.length);
        var TOTAL_COLS = 9, COL_PERIOD = 1, COL_MON = 2;
        var rowspanRemaining = {};
        var courses = [];

        // ========== 2. 逐行解析 ==========
        for (var r = 0; r < allRows.length; r++) {
            var row = allRows[r];
            var cells = row.querySelectorAll('td, th');
            if (cells.length < 2) continue;

            var updatedRemaining = {};
            for (var key in rowspanRemaining) {
                if (rowspanRemaining.hasOwnProperty(key)) {
                    var left = rowspanRemaining[key] - 1;
                    if (left > 0) updatedRemaining[key] = left;
                }
            }
            rowspanRemaining = updatedRemaining;

            var cellIdx = 0, realCol = 0, periodNum = -1;
            var courseCells = [];

            while (cellIdx < cells.length && realCol < TOTAL_COLS) {
                while (realCol < TOTAL_COLS && rowspanRemaining.hasOwnProperty('' + realCol)) {
                    realCol++;
                }
                if (realCol >= TOTAL_COLS) break;

                var cell = cells[cellIdx];
                var rs = parseInt(cell.getAttribute('rowspan') || cell.getAttribute('rowSpan') || '1', 10);
                if (rs > 1) rowspanRemaining['' + realCol] = rs;

                if (realCol === COL_PERIOD) {
                    var pText = (cell.innerText || '').replace(/[^0-9]/g, '');
                    var pn = parseInt(pText, 10);
                    if (pn >= 1 && pn <= 15) periodNum = pn;
                }

                if (realCol >= COL_MON && realCol <= 8) {
                    var cleanLines = htmlToLines(cell.innerHTML || '');
                    if (cleanLines.length >= 3) {
                        var dayOfWeek = realCol - 1;
                        courseCells.push({dayOfWeek: dayOfWeek, lines: cleanLines});
                    }
                }

                cellIdx++;
                realCol++;
            }

            if (periodNum >= 1 && courseCells.length > 0) {
                var parts = [];
                for (var dc2 = 0; dc2 < courseCells.length; dc2++) {
                    var cl = courseCells[dc2].lines;
                    var cn2 = cl[0].replace(/[☆●◆★♦]/g, '').substring(0, 8);
                    // 在行1~3中搜索周次行（调课可能多一个标签行导致索引偏移）
                    var pw2 = cl[1] || '';
                    for (var si2 = 1; si2 < Math.min(cl.length, 4); si2++) {
                        if (/(\d+)周|\((\d+)-(\d+)节\)/.test(cl[si2])) { pw2 = cl[si2]; break; }
                    }
                    var wm2 = pw2.match(/(\d+)-(\d+)周/g);
                    var wk;
                    if (wm2) {
                        // 显示所有段（如 "1-7周,9-12周"）
                        var ranges = [];
                        for (var wi3 = 0; wi3 < wm2.length; wi3++) ranges.push(wm2[wi3]);
                        wk = ranges.join(',');
                    } else {
                        wk = (pw2.match(/(\d+)周/) || ['?RAW:' + pw2.replace(/\|/g,'').substring(0, 30)])[0];
                    }
                    parts.push('D' + courseCells[dc2].dayOfWeek + ':' + cn2 + '[' + wk + ']');
                }
                diagLines.push('P' + periodNum + ' ' + parts.join(' '));
            }

            if (periodNum < 1 || courseCells.length === 0) continue;

            // 解析课程
            for (var dc = 0; dc < courseCells.length; dc++) {
                var cellData = courseCells[dc];
                var cleanLines = cellData.lines;

                // 提取共享字段
                var baseName = cleanLines[0].replace(/[☆●◆★♦]/g, '').trim() || '未识别';
                var baseStartPeriod = periodNum;
                var baseEndPeriod = periodNum + 1;

                // 搜索周次/节次行
                var pwIdx = -1, pwLine = '';
                for (var si = 1; si < Math.min(cleanLines.length, 5); si++) {
                    var candidate = cleanLines[si] || '';
                    if (/\((\d+)-(\d+)节\)/.test(candidate) || /(\d+)周/.test(candidate)) {
                        pwLine = candidate; pwIdx = si; break;
                    }
                }
                if (!pwLine) pwLine = cleanLines[1] || '';

                var pm = pwLine.match(/\((\d+)-(\d+)节\)/);
                if (pm) { baseStartPeriod = parseInt(pm[1], 10); baseEndPeriod = parseInt(pm[2], 10); }

                // 教室：搜索含"校区"的行
                var baseClassroom = cleanLines.length > 2 ? cleanLines[2].trim() : '';
                for (var si3 = 2; si3 < Math.min(cleanLines.length, 6); si3++) {
                    if (cleanLines[si3].indexOf('校区') >= 0) { baseClassroom = cleanLines[si3].trim(); break; }
                }

                // 教师：搜索中文名
                var baseTeacher = '';
                if (pwIdx >= 0) {
                    for (var si4 = pwIdx + 1; si4 < Math.min(cleanLines.length, 7); si4++) {
                        var tl = cleanLines[si4].trim();
                        if (!/-\d{4}$/.test(tl) && tl.length >= 2 && tl.length < 6 && /^[一-龥]+$/.test(tl)) {
                            baseTeacher = tl; break;
                        }
                    }
                }

                // 解析所有周次范围
                var allRanges = [];
                var wm = pwLine.match(/(\d+)-(\d+)周/g);
                if (wm) {
                    for (var wi = 0; wi < wm.length; wi++) {
                        var f = wm[wi].match(/(\d+)-(\d+)/);
                        if (f) allRanges.push([parseInt(f[1], 10), parseInt(f[2], 10)]);
                    }
                } else {
                    var sm = pwLine.match(/(\d+)周/);
                    if (sm) allRanges.push([parseInt(sm[1], 10), parseInt(sm[1], 10)]);
                }
                if (allRanges.length === 0) allRanges.push([1, 20]);

                // 每段周次创建一门课
                for (var ri = 0; ri < allRanges.length; ri++) {
                    var range = allRanges[ri];
                    var course = {
                        courseName: baseName,
                        teacher: baseTeacher,
                        classroom: baseClassroom,
                        dayOfWeek: cellData.dayOfWeek,
                        startPeriod: baseStartPeriod,
                        endPeriod: baseEndPeriod,
                        startWeek: range[0],
                        endWeek: range[1],
                        weekType: 'ALL', color: getColor(baseName)
                    };
                    if (/单/.test(pwLine)) course.weekType = 'ODD';
                    else if (/双/.test(pwLine)) course.weekType = 'EVEN';

                    // 去重（含 startWeek）
                    var dup = false;
                    for (var di = 0; di < courses.length; di++) {
                        var c = courses[di];
                        if (c.courseName === course.courseName && c.dayOfWeek === course.dayOfWeek &&
                            c.startPeriod === course.startPeriod && c.startWeek === course.startWeek) {
                            dup = true; break;
                        }
                    }
                    if (!dup) courses.push(course);
                }
            }
        }

        // ========== 3. 备选扫描（如果主方案没找到课程） ==========
        if (courses.length === 0) {
            diagLines.push('FB');
            var allTds = targetTable.querySelectorAll('td');
            for (var si = 0; si < allTds.length; si++) {
                var td = allTds[si];
                var cleanLines = htmlToLines(td.innerHTML || '');
                if (cleanLines.length < 3) continue;
                // 检查是否像课程
                var joined = cleanLines.join(' ');
                if (joined.indexOf('节') < 0 || joined.indexOf('周') < 0) continue;

                var fbName = cleanLines[0].replace(/[☆●◆★♦]/g, '').trim() || '未识别';
                var course = {
                    courseName: fbName,
                    teacher: '', classroom: cleanLines.length > 2 ? cleanLines[2].trim() : '',
                    dayOfWeek: 0, startPeriod: 1, endPeriod: 2,
                    startWeek: 1, endWeek: 20, weekType: 'ALL', color: getColor(fbName)
                };
                // 搜索周次行（不固定索引，兼容调课格式）
                var pwLine = cleanLines[1] || '';
                for (var si5 = 1; si5 < Math.min(cleanLines.length, 4); si5++) {
                    if (/(\d+)周|\((\d+)-(\d+)节\)/.test(cleanLines[si5])) { pwLine = cleanLines[si5]; break; }
                }
                var pm = pwLine.match(/\((\d+)-(\d+)节\)/);
                if (pm) { course.startPeriod = parseInt(pm[1], 10); course.endPeriod = parseInt(pm[2], 10); }
                var wm = pwLine.match(/(\d+)-(\d+)周/g);
                if (wm) {
                    var minW = 99, maxW = 0;
                    for (var wi2 = 0; wi2 < wm.length; wi2++) {
                        var f2 = wm[wi2].match(/(\d+)-(\d+)/);
                        if (f2) { var ws = parseInt(f2[1], 10), we = parseInt(f2[2], 10); if (ws < minW) minW = ws; if (we > maxW) maxW = we; }
                    }
                    course.startWeek = minW; course.endWeek = maxW;
                }
                else { var sm2 = pwLine.match(/(\d+)周/); if (sm2) { var w2 = parseInt(sm2[1], 10); course.startWeek = w2; course.endWeek = w2; } }
                if (/单/.test(pwLine)) course.weekType = 'ODD';
                else if (/双/.test(pwLine)) course.weekType = 'EVEN';
                if (cleanLines.length > 3) {
                    var tl = cleanLines[3].trim();
                    if (!/-\d{4}$/.test(tl) && tl.length < 6) course.teacher = tl;
                }
                // x坐标推断dayOfWeek
                try {
                    var tdRect = td.getBoundingClientRect();
                    var tcx = tdRect.left + tdRect.width / 2;
                    var bestDay = 0, bestDist = 9999;
                    var hdrCells = allRows[0] ? allRows[0].querySelectorAll('td, th') : [];
                    for (var hi = 2; hi < Math.min(hdrCells.length, 9); hi++) {
                        var hr = hdrCells[hi].getBoundingClientRect();
                        var dist = Math.abs(tcx - (hr.left + hr.width / 2));
                        if (dist < bestDist && dist < 200) { bestDist = dist; bestDay = hi - 1; }
                    }
                    if (bestDay > 0) course.dayOfWeek = bestDay;
                } catch(e2) {}

                var dup = false;
                for (var di = 0; di < courses.length; di++) {
                    var c = courses[di];
                    if (c.courseName === course.courseName && c.dayOfWeek === course.dayOfWeek && c.startPeriod === course.startPeriod) {
                        dup = true; break;
                    }
                }
                if (!dup) { courses.push(course); diagLines.push('FB:' + course.courseName); }
            }
        }

        // ========== 4. 补充课程 ==========
        var bodyText = (document.body.innerText || '');
        var otherMatch = bodyText.match(/其它课程[：:]\s*(.+)/);
        if (otherMatch) {
            var extraText = otherMatch[1];
            var extraParts = extraText.split(/[;；]/);
            for (var ep = 0; ep < extraParts.length; ep++) {
                var part = extraParts[ep].trim();
                if (!part) continue;
                var em = part.match(/([^●◆]+?)\s*[●◆]\s*([^(]+)\(共(\d+)周\)\/(\d+)-(\d+)周/);
                if (em) {
                    courses.push({
                        courseName: em[1].trim(), teacher: em[2].trim(), classroom: '待定',
                        dayOfWeek: 0, startPeriod: 0, endPeriod: 0,
                        startWeek: parseInt(em[4], 10), endWeek: parseInt(em[5], 10),
                        weekType: 'ALL', color: getColor(em[1].trim())
                    });
                }
            }
        }

        // ========== 5. 返回（精简 diag，防截断） ==========
        var names = [];
        for (var ni = 0; ni < courses.length; ni++) names.push(courses[ni].courseName);
        diagLines.push('=' + courses.length + ':' + names.join(','));

        return JSON.stringify({
            success: courses.length > 0,
            courses: courses,
            count: courses.length,
            diag: diagLines.join('|')
        });

    } catch (e) {
        return JSON.stringify({ error: 'ERR:' + e.message });
    }
})();
""".trimIndent()

    /**
     * 将 JS 返回的 JSON 字符串解析为 Course 列表
     *
     * @param jsonResult JS evaluateJavascript 回调的原始字符串
     * @return Pair<课程列表, 错误信息>
     */
    data class ParseResult(
        val courses: List<Course>,
        val error: String?,
        val diagnostic: String?  // 诊断信息，用于排查问题
    )

    fun parseJsonResult(jsonResult: String): ParseResult {
        return try {
            var json = jsonResult.trim()
            if (json.startsWith("\"") && json.endsWith("\"")) {
                json = json.substring(1, json.length - 1)
                    .replace("\\\"", "\"")
            }

            val root = JSONObject(json)
            val diag = root.optString("diag", "").ifEmpty { null }

            if (root.has("error")) {
                return ParseResult(emptyList(), root.getString("error"), diag)
            }

            val arr = root.getJSONArray("courses")
            val courses = mutableListOf<Course>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                // 跳过 dayOfWeek=0 的补充课程（实训等无固定时间的课程）
                val rawDay = obj.optInt("dayOfWeek", 1)
                if (rawDay == 0) continue
                courses.add(
                    Course(
                        courseName = obj.optString("courseName", ""),
                        teacher = obj.optString("teacher", ""),
                        classroom = obj.optString("classroom", ""),
                        dayOfWeek = rawDay.coerceIn(1, 7),
                        startPeriod = obj.optInt("startPeriod", 1).coerceIn(1, 12),
                        endPeriod = obj.optInt("endPeriod", 1).coerceIn(1, 12),
                        startWeek = obj.optInt("startWeek", 1).coerceIn(1, 25),
                        endWeek = obj.optInt("endWeek", 20).coerceIn(1, 25),
                        weekType = WeekType.fromDbValue(obj.optString("weekType", "ALL")),
                        colorValue = obj.optLong("color", 0xFFA3B5A6)
                    )
                )
            }
            ParseResult(
                courses = courses,
                error = if (courses.isEmpty()) "未识别到课程，请确认课表已完整展示" else null,
                diagnostic = diag
            )
        } catch (e: Exception) {
            ParseResult(emptyList(), "解析结果失败：${e.message}", null)
        }
    }
}
