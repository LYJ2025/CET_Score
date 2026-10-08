package com.cetscore.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.cetscore.core.ui.theme.Dimens

/**
 * 备注输入对话框。
 *
 * 备注长度不限制，所以：
 *  - 输入框支持多行 + 内部滚动
 *  - 高度有上限但可滚动，避免超长备注把对话框撑出屏幕
 *
 * @param initialValue 已有备注（编辑时传入，新建时为 null）
 * @param onConfirm 确认回调，参数为最终备注文本
 * @param onDismiss 取消回调
 */
@Composable
fun NoteInputDialog(
    initialValue: String? = null,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialValue == null) "添加备注" else "编辑备注",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = "记录本次估分的情况，例如题感、状态、下次目标。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.SpaceM))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        // 限制高度并允许内部滚动，长备注不会把弹窗撑爆
                        .heightIn(min = 120.dp, max = 240.dp),
                    placeholder = { Text("例如：阅读仔细阅读错了 3 道，作文模板不太贴题") },
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
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(
                    onClick = {
                        // 空字符串视为无备注，存 null 而非 ""
                        onConfirm(text.trim().ifEmpty { null })
                    },
                ) {
                    Text("保存")
                }
            }
        },
        shape = RoundedCornerShape(Dimens.CornerMedium),
    )
}
