package com.example.simpleschedule.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider

/**
 * APK 下载安装器
 *
 * 使用系统 DownloadManager 下载 → 下载完成自动调起安装界面。
 * 用户只需点一次"安装"，无需手动找文件。
 */
object ApkInstaller {

    private const val FILE_NAME = "courseapp_update.apk"

    /**
     * 下载并安装 APK
     * @param context Context
     * @param url APK 下载地址
     */
    fun downloadAndInstall(context: Context, url: String): Long {
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri).apply {
            setTitle("课程通更新下载中")
            setDescription("正在下载最新版本…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        // 注册下载完成广播
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    ctx.unregisterReceiver(this)
                    install(ctx, downloadId)
                }
            }
        }, filter)

        return downloadId
    }

    /** 调起系统安装界面 */
    private fun install(context: Context, downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(downloadId) ?: return

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7+ 通过 FileProvider
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    java.io.File(uri.path ?: return)
                )
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
