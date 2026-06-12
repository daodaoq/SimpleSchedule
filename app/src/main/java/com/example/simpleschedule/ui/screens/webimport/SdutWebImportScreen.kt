package com.example.simpleschedule.ui.screens.webimport

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.simpleschedule.domain.model.Course
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SdutWebImportScreen(
    onBack: () -> Unit,
    onCoursesParsed: (List<Course>) -> Unit,
    school: SchoolInfo
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(school.jwUrl) }
    var isExtracting by remember { mutableStateOf(false) }
    var parsedCourses by remember { mutableStateOf<List<Course>?>(null) }
    val isOnTablePage = currentUrl.contains(school.urlKeyword)

    // 提取课表
    fun doExtract() {
        val wv = webView ?: return
        isExtracting = true
        parsedCourses = null
        wv.evaluateJavascript(SdutTableParser.JS_PARSE_SCRIPT) { raw ->
            isExtracting = false
            val result = SdutTableParser.parseJsonResult(raw)
            Log.d("SdutParser", "diag=${result.diagnostic}")
            Log.d("SdutParser", "courses=${result.courses.size} ${result.courses.map { it.courseName }}")
            if (result.courses.isNotEmpty()) {
                parsedCourses = result.courses
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(result.error ?: "未识别到课程")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课表导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ---- WebView ----
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null) currentUrl = url
                            }
                        }

                        loadUrl(school.jwUrl)
                    }
                },
                update = { wv -> webView = wv },
                modifier = Modifier.fillMaxSize()
            )

            // ---- 底部按钮 ----
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isExtracting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("正在解析…", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (!isOnTablePage) {
                        Text(
                            "👆 登录后：信息查询 → 个人课表查询",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { doExtract() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOnTablePage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth(0.7f).height(48.dp)
                    ) {
                        Text(
                            if (isOnTablePage) "📋 一键提取课表" else "📋 请先导航到课表页",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---- 解析结果预览（在 WebView 上方） ----
            if (parsedCourses != null) {
                val courses = parsedCourses!!
                // 半透明遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "解析完成 ✅",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "共 ${courses.size} 门课程",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            courses.take(8).forEach { c ->
                                Text(
                                    text = "${c.courseName} · 周${c.dayOfWeek} ${c.startPeriod}-${c.endPeriod}节 · ${c.teacher}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (courses.size > 8) {
                                Text(
                                    text = "…等 ${courses.size} 门",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val toImport = courses
                                    parsedCourses = null
                                    try {
                                        onCoursesParsed(toImport)
                                    } catch (e: Exception) {
                                        Log.e("SdutParser", "import crash", e)
                                        scope.launch { snackbarHostState.showSnackbar("导入失败: ${e.message}") }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                            Text("确认导入（${courses.size}门）", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { parsedCourses = null },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    }
                }
            }
        }
    }
}
