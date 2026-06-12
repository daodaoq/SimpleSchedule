package com.example.simpleschedule.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Releases 更新检查器
 *
 * 通过 GitHub API 查询最新 Release，与当前版本对比。
 * 免费、无需服务器、无需 Google Play。
 */
object UpdateChecker {

    private const val GITHUB_API = "https://api.github.com/repos/daodaoq/SimpleSchedule/releases/latest"
    private const val CURRENT_VERSION = "1.2.0"  // 与 GitHub Release tag 对比

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String = "",
        val downloadUrl: String = "",
        val releaseNotes: String = ""
    )

    /**
     * 检查是否有新版本
     */
    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (conn.responseCode != 200) return@withContext UpdateInfo(false)

            val json = conn.inputStream.bufferedReader().readText()
            val root = JSONObject(json)
            val tag = root.getString("tag_name").removePrefix("v")
            val assets = root.getJSONArray("assets")
            val downloadUrl = if (assets.length() > 0) {
                assets.getJSONObject(0).getString("browser_download_url")
            } else ""
            val body = root.optString("body", "")

            val hasUpdate = compareVersions(tag, CURRENT_VERSION) > 0
            UpdateInfo(hasUpdate, tag, downloadUrl, body)
        } catch (e: Exception) {
            UpdateInfo(false) // 网络异常静默处理
        }
    }

    /** 版本号比较：>0 表示 v1 > v2 */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }
}
