package com.cetscore.feature.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

/**
 * 把文本写入系统剪贴板。
 *
 * 单独抽出来让页面主体保持"无副作用"，
 * 让剪贴板写入这个动作明确可见、可测试。
 */
@Composable
fun rememberClipboardCopier(): (String) -> Unit {
    val clipboard = LocalClipboardManager.current
    return remember(clipboard) { { text: String -> clipboard.setText(AnnotatedString(text)) } }
}
