package com.example.simpleschedule.ui.screens.webimport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 学校信息
 */
data class SchoolInfo(
    val name: String,
    val jwUrl: String,
    val urlKeyword: String
)

/** 已适配的学校列表 */
val SUPPORTED_SCHOOLS = listOf(
    SchoolInfo(
        name = "山东理工大学",
        jwUrl = "https://jw.sdut.edu.cn",
        urlKeyword = "xskbcx_cxXskbcxIndex"
    )
)

/**
 * 导入方式选择页
 *
 * 两种导入方式：
 * 1. 教务处官网导入 → 选择学校 → WebView 登录提取
 * 2. PDF 文件导入 → 选择 PDF → 自动解析
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSelectScreen(
    onBack: () -> Unit,
    onSchoolSelected: (SchoolInfo) -> Unit,
    onPdfImport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入课表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // ===== PDF 导入（下个版本开放）=====
            // TODO: 下个版本恢复 PDF 导入入口

            // ===== 教务处官网导入 =====
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "教务处官网导入",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "登录教务系统自动提取，请选择你的学校",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // ===== 学校列表 =====
            items(SUPPORTED_SCHOOLS) { school ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSchoolSelected(school) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            BoxWithCenter {
                                Text("🏫", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = school.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "已适配",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 居中 Box 的便捷封装 */
@Composable
private fun BoxWithCenter(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
