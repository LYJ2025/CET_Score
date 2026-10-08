package com.cetsix.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cetsix.core.ui.theme.Dimens

/**
 * 历史页的备注编辑弹窗。
 *
 * 与结果页的 NoteInputDialog 行为一致，但独立实现 ——
 * 这样两个 feature 模块互不依赖，各自演进。
 * 若后续想复用，可上提到 core:ui。
 */
@Composable
fun NoteEditDialog(
    initialValue: String?,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑备注", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        // 限高 + 可滚动，长备注不会把弹窗撑出屏幕
                        .heightIn(min = 120.dp, max = 240.dp),
                    placeholder = { Text("记录本次估分的情况") },
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                    maxLines = 8,
                )
                Spacer(Modifier.height(Dimens.SpaceS))
                Text(
                    text = "${text.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 空内容视为删除备注
                    onConfirm(text.trim().ifEmpty { null })
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = RoundedCornerShape(Dimens.CornerMedium),
    )
}
