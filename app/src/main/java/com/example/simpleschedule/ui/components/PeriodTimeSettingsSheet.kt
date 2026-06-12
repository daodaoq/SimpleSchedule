package com.example.simpleschedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.simpleschedule.domain.model.PeriodTime
import kotlinx.coroutines.launch

/**
 * 节次时间设置弹窗
 *
 * 以 ModalBottomSheet 展示 12 节课的开始/结束时间，
 * 用户可逐节修改，保存后即时生效到课程表网格左侧标签。
 *
 * @param currentTimes 当前节次时间表
 * @param sheetState 底部弹窗状态
 * @param onDismiss 关闭弹窗
 * @param onSave 保存回调，传入新的时间表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodTimeSettingsSheet(
    currentTimes: List<PeriodTime>,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSave: (List<PeriodTime>) -> Unit
) {
    val scope = rememberCoroutineScope()

    // 本地表单状态：深拷贝当前时间表
    var times by remember(currentTimes) {
        mutableStateOf(currentTimes.map { it.copy() })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ---- 标题 ----
            Text(
                text = "上课时间设置",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ---- 表头 ----
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text("节次", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(48.dp))
                Text("开始", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Text("结束", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- 12 行时间输入 ----
            times.forEachIndexed { index, pt ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 节次编号
                    Text(
                        text = "${pt.period}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // 开始时间
                    OutlinedTextField(
                        value = pt.startTime,
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { it.isDigit() || it == ':' }.take(5)
                            times = times.toMutableList().also {
                                it[index] = pt.copy(startTime = filtered)
                            }
                        },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    // 结束时间
                    OutlinedTextField(
                        value = pt.endTime,
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { it.isDigit() || it == ':' }.take(5)
                            times = times.toMutableList().also {
                                it[index] = pt.copy(endTime = filtered)
                            }
                        },
                        placeholder = { Text("08:45") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    // 删除按钮（至少保留 1 节）
                    if (times.size > 1) {
                        IconButton(
                            onClick = {
                                times = times.toMutableList().also {
                                    it.removeAt(index)
                                    // 重新编号
                                    it.forEachIndexed { i, t -> it[i] = t.copy(period = i + 1) }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除此节",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(32.dp))
                    }
                }
            }

            // ---- 添加一节按钮 ----
            OutlinedButton(
                onClick = {
                    if (times.size < 20) {
                        val newPeriod = times.size + 1
                        times = times.toMutableList().also {
                            it.add(PeriodTime(period = newPeriod, startTime = "", endTime = ""))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加一节")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 操作按钮 ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { times = PeriodTime.DEFAULTS.map { it.copy() } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("恢复默认")
                }
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }
                        onSave(times)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
