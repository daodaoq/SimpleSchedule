package com.example.simpleschedule.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * 单节课的时间段
 *
 * @param period    节次编号（1-12）
 * @param startTime 开始时间（如 "08:00"）
 * @param endTime   结束时间（如 "08:45"）
 */
data class PeriodTime(
    val period: Int,
    val startTime: String,
    val endTime: String
) {
    companion object {
        /** 默认 8 节课，上午4节+下午4节，45分钟/节，大课间25分钟 */
        val DEFAULTS: List<PeriodTime> = listOf(
            PeriodTime(1,  "08:00", "08:45"),
            PeriodTime(2,  "08:55", "09:40"),
            PeriodTime(3,  "10:05", "10:50"),  // 大课间 25min
            PeriodTime(4,  "11:00", "11:45"),
            PeriodTime(5,  "14:00", "14:45"),  // 下午开始
            PeriodTime(6,  "14:55", "15:40"),
            PeriodTime(7,  "16:05", "16:50"),  // 大课间 25min
            PeriodTime(8,  "17:00", "17:45")
        )

        // ========== JSON 序列化 ==========

        fun toJson(list: List<PeriodTime>): String {
            val arr = JSONArray()
            list.forEach { pt ->
                arr.put(JSONObject().apply {
                    put("period", pt.period)
                    put("start", pt.startTime)
                    put("end", pt.endTime)
                })
            }
            return arr.toString()
        }

        fun fromJson(json: String): List<PeriodTime> {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    PeriodTime(
                        period = obj.getInt("period"),
                        startTime = obj.getString("start"),
                        endTime = obj.getString("end")
                    )
                }
            } catch (e: Exception) {
                DEFAULTS
            }
        }
    }
}

/** 根据节次编号查找对应时间，找不到返回 null */
fun List<PeriodTime>.timeFor(period: Int): PeriodTime? =
    firstOrNull { it.period == period }

