package com.example.simpleschedule.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.simpleschedule.domain.model.PeriodTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 顶层扩展属性：每个 Context 对应一个 DataStore 单例 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule_prefs")

/**
 * 轻量级偏好设置管理器
 *
 * 存储内容：
 * - 学期开始日期（ISO 格式字符串）
 * - 手动周偏移量
 * - 节次时间表（JSON 字符串）
 */
class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_SEMESTER_START = stringPreferencesKey("semester_start_date")
        private val KEY_WEEK_OFFSET = intPreferencesKey("week_offset")
        private val KEY_PERIOD_TIMES = stringPreferencesKey("period_times")
        private val KEY_CARD_OPACITY = intPreferencesKey("card_opacity")      // 0-100
        private val KEY_CARD_FONT_SCALE = intPreferencesKey("card_font_scale") // 80-130 (%)
        private val KEY_CARD_CORNER = intPreferencesKey("card_corner")         // 4-12 dp
        private val KEY_SHOW_TEACHER = intPreferencesKey("show_teacher")       // 0/1
        private val KEY_SHOW_CLASSROOM = intPreferencesKey("show_classroom")   // 0/1

        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /** 卡片全局设置流 */
    val cardSettings: Flow<CardSettings> = context.dataStore.data.map { prefs ->
        CardSettings(
            opacity = (prefs[KEY_CARD_OPACITY] ?: 100) / 100f,
            fontScale = (prefs[KEY_CARD_FONT_SCALE] ?: 100) / 100f,
            cornerRadius = prefs[KEY_CARD_CORNER] ?: 8,
            showTeacher = (prefs[KEY_SHOW_TEACHER] ?: 1) == 1,
            showClassroom = (prefs[KEY_SHOW_CLASSROOM] ?: 1) == 1
        )
    }

    suspend fun setCardSettings(settings: CardSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CARD_OPACITY] = (settings.opacity * 100).toInt()
            prefs[KEY_CARD_FONT_SCALE] = (settings.fontScale * 100).toInt()
            prefs[KEY_CARD_CORNER] = settings.cornerRadius
            prefs[KEY_SHOW_TEACHER] = if (settings.showTeacher) 1 else 0
            prefs[KEY_SHOW_CLASSROOM] = if (settings.showClassroom) 1 else 0
        }
    }

    data class CardSettings(
        val opacity: Float = 1f,          // 0.3-1.0
        val fontScale: Float = 1f,         // 0.8-1.3
        val cornerRadius: Int = 8,         // 4-12 dp
        val showTeacher: Boolean = true,
        val showClassroom: Boolean = true
    )

    /** 学期开始日期 */
    val semesterStartDate: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SEMESTER_START]?.let { dateStr ->
            try { LocalDate.parse(dateStr, DATE_FORMATTER) }
            catch (e: Exception) { null }
        }
    }

    /** 手动周偏移量 */
    val weekOffset: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEEK_OFFSET] ?: 0
    }

    /** 节次时间表（JSON → List<PeriodTime>，未设置时返回默认值） */
    val periodTimes: Flow<List<PeriodTime>> = context.dataStore.data.map { prefs ->
        prefs[KEY_PERIOD_TIMES]?.let { PeriodTime.fromJson(it) } ?: PeriodTime.DEFAULTS
    }

    suspend fun setSemesterStartDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SEMESTER_START] = date.format(DATE_FORMATTER)
        }
    }

    suspend fun setWeekOffset(offset: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WEEK_OFFSET] = offset
        }
    }

    /** 持久化节次时间表 */
    suspend fun setPeriodTimes(times: List<PeriodTime>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PERIOD_TIMES] = PeriodTime.toJson(times)
        }
    }
}
