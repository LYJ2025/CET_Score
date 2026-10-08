package com.cetsix.feature.aiassistant.pro

import android.content.Context

/**
 * API 配置存储 —— 用 SharedPreferences（应用私有目录）。
 *
 * 为什么不用 EncryptedSharedPreferences：
 *  1. 它要求 API 23+ 且在部分 Android 8 机型上存在已知兼容问题
 *  2. 会引入 androidx.security 依赖，只为存三个字符串不划算
 *  3. 这是**用户自己填的自己的 API Key**，存在应用私有目录已被沙箱保护
 *
 * 若后续要更强的保护，可在此处加一层对称加密（密钥存 Keystore）。
 */
class ApiConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences("ai_api_config", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL).orEmpty()
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty()
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    /** 是否已完成必要配置 */
    val isConfigured: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    /** 清空配置 */
    fun clear() = prefs.edit().clear().apply()

    /** 校验配置，缺失时给出明确提示（不静默失败） */
    fun validate(): String? = when {
        apiKey.isBlank() -> "未配置 API Key，请到设置页填写"
        baseUrl.isBlank() -> "未配置 Base URL，请到设置页填写"
        model.isBlank() -> "未配置模型名，请到设置页填写"
        !baseUrl.startsWith("http") -> "Base URL 需以 http:// 或 https:// 开头"
        else -> null
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"

        /** 默认 DeepSeek（OpenAI 兼容协议） */
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}