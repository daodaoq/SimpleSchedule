package com.example.simpleschedule.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 更新检查器 — Gitee 优先（国内快），GitHub 兜底
 */
object UpdateChecker {

    private const val GITEE_API =
        "https://gitee.com/api/v5/repos/daodaoq/SimpleSchedule/releases/latest"
    private const val GITHUB_API =
        "https://api.github.com/repos/daodaoq/SimpleSchedule/releases/latest"
    private const val CURRENT_VERSION = "2.0.0"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String = "",
        val downloadUrl: String = "",
        val releaseNotes: String = ""
    )

    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        tryGitee() ?: tryGitHub() ?: UpdateInfo(false)
    }

    private fun tryGitee(): UpdateInfo? {
        return try {
            val conn = URL(GITEE_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            if (conn.responseCode != 200) return null
            val json = conn.inputStream.bufferedReader().readText()
            val root = JSONObject(json)
            val tag = root.getString("tag_name").removePrefix("v")
            val assets = root.optJSONArray("assets") ?: root.optJSONArray("attach_files")
            val downloadUrl = if (assets != null && assets.length() > 0) {
                assets.getJSONObject(0).optString("browser_download_url", "")
            } else ""
            val body = root.optString("body", "")
            if (compareVersions(tag, CURRENT_VERSION) > 0) UpdateInfo(true, tag, downloadUrl, body)
            else UpdateInfo(false)
        } catch (_: Exception) { null }
    }

    private fun tryGitHub(): UpdateInfo? {
        return try {
            val conn = URL(GITHUB_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000; conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            if (conn.responseCode != 200) return null
            val json = conn.inputStream.bufferedReader().readText()
            val root = JSONObject(json)
            val tag = root.getString("tag_name").removePrefix("v")
            val arr = root.getJSONArray("assets")
            val downloadUrl = if (arr.length() > 0) arr.getJSONObject(0).getString("browser_download_url") else ""
            val body = root.optString("body", "")
            if (compareVersions(tag, CURRENT_VERSION) > 0) UpdateInfo(true, tag, downloadUrl, body)
            else UpdateInfo(false)
        } catch (_: Exception) { null }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val p1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val d = p1.getOrElse(i) { 0 } - p2.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}
