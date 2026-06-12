package com.example.simpleschedule.data.local

import android.content.Context
import com.example.simpleschedule.domain.model.Course
import com.example.simpleschedule.domain.model.WeekType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 课程 JSON 文件存储
 *
 * 设计理念：对于课程表这种小数据量场景（通常 10~30 门课），
 * JSON 文件存储比 Room 数据库更轻量、更快、更简单：
 * - 零额外依赖：使用 Android 内置 org.json 库
 * - 零注解处理器：无需 kapt/KSP，编译速度极快
 * - 极简实现：全部代码 < 100 行，易维护
 * - 响应式更新：MutableStateFlow 驱动 UI 自动刷新
 *
 * 存储路径：{filesDir}/courses.json
 */
class CourseStore(private val context: Context) {

    private val storeFile: File
        get() = File(context.filesDir, "courses.json")

    /** 课程列表的可变状态流，任何变更自动通知 UI */
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    /** 自增 ID 计数器 */
    private var nextId: Long = 1L

    /**
     * 从文件加载全部课程
     * 应在应用启动时调用一次，后续通过 StateFlow 响应式获取
     */
    suspend fun loadAll(): List<Course> = withContext(Dispatchers.IO) {
        try {
            if (!storeFile.exists()) {
                _courses.value = emptyList()
                return@withContext emptyList()
            }
            val json = storeFile.readText()
            val array = JSONArray(json)
            val list = mutableListOf<Course>()
            var maxId = 0L
            for (i in 0 until array.length()) {
                val course = parseCourse(array.getJSONObject(i))
                list.add(course)
                if (course.id > maxId) maxId = course.id
            }
            // 按节次排序
            list.sortWith(compareBy({ it.startPeriod }, { it.dayOfWeek }))
            nextId = maxId + 1
            _courses.value = list
            list
        } catch (e: Exception) {
            e.printStackTrace()
            _courses.value = emptyList()
            emptyList()
        }
    }

    /**
     * 保存课程（新增/更新）
     * id == 0 → 新增（自动分配 ID）
     * id != 0 → 更新已有课程
     */
    suspend fun save(course: Course): Long = withContext(Dispatchers.IO) {
        val current = _courses.value.toMutableList()
        val resultId: Long
        if (course.id == 0L) {
            val newCourse = course.copy(id = nextId++)
            current.add(newCourse)
            resultId = newCourse.id
        } else {
            val index = current.indexOfFirst { it.id == course.id }
            if (index >= 0) {
                current[index] = course
            } else {
                current.add(course)
            }
            resultId = course.id
        }
        current.sortBy { it.startPeriod }
        _courses.value = current
        persist(current)
        resultId
    }

    /**
     * 批量保存课程（只写一次文件，比逐个 save 高效且安全）
     */
    suspend fun saveAll(courses: List<Course>) = withContext(Dispatchers.IO) {
        val current = _courses.value.toMutableList()
        courses.forEach { course ->
            val newCourse = course.copy(id = nextId++)
            current.add(newCourse)
        }
        current.sortBy { it.startPeriod }
        _courses.value = current
        persist(current)
    }

    /**
     * 清空所有课程
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        _courses.value = emptyList()
        nextId = 1L
        persist(emptyList())
    }

    /**
     * 删除课程
     */
    suspend fun delete(course: Course) = withContext(Dispatchers.IO) {
        val current = _courses.value.toMutableList()
        current.removeAll { it.id == course.id }
        _courses.value = current
        persist(current)
    }

    // ===================== 内部辅助方法 =====================

    /** 持久化到 JSON 文件 */
    private fun persist(courses: List<Course>) {
        try {
            val array = JSONArray()
            courses.forEach { course ->
                array.put(serializeCourse(course))
            }
            storeFile.writeText(array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 从 JSONObject 解析 Course */
    private fun parseCourse(obj: JSONObject): Course = Course(
        id = obj.optLong("id", 0),
        courseName = obj.getString("courseName"),
        teacher = obj.optString("teacher", ""),
        classroom = obj.optString("classroom", ""),
        dayOfWeek = obj.getInt("dayOfWeek").coerceIn(1, 7),
        startPeriod = obj.getInt("startPeriod").coerceIn(1, 12),
        endPeriod = obj.getInt("endPeriod").coerceIn(1, 12),
        startWeek = obj.getInt("startWeek").coerceIn(1, 25),
        endWeek = obj.getInt("endWeek").coerceIn(1, 25),
        weekType = WeekType.fromDbValue(obj.optString("weekType", "ALL")),
        colorValue = obj.optLong("color", 0xFFA3B5A6)
    )

    /** 将 Course 序列化为 JSONObject */
    private fun serializeCourse(course: Course): JSONObject = JSONObject().apply {
        put("id", course.id)
        put("courseName", course.courseName)
        put("teacher", course.teacher)
        put("classroom", course.classroom)
        put("dayOfWeek", course.dayOfWeek)
        put("startPeriod", course.startPeriod)
        put("endPeriod", course.endPeriod)
        put("startWeek", course.startWeek)
        put("endWeek", course.endWeek)
        put("weekType", course.weekType.dbValue)
        put("color", course.colorValue)
    }
}
