package com.cetscore.feature.aiassistant.pro

import android.content.Context

/**
 * 一套 AI 接口配置。
 *
 * @param slot 槽位（0..[MAX_PROFILES]-1），用于稳定标识一套配置
 * @param name 用户起的名字，例如「DeepSeek 官方」「学校代理」
 */
data class AiProfile(
    val slot: Int,
    val name: String,
    val apiKey: String,
    val baseUrl: String,
    val model: String,
) {
    /** 是否填齐了必要字段 */
    val isComplete: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    /** 列表里显示的标题：没起名时退化为模型名 */
    val displayName: String
        get() = name.ifBlank { model.ifBlank { "未命名配置" } }
}

/**
 * AI 配置存储 —— 用 SharedPreferences（应用私有目录）。
 *
 * ## 多套配置
 *
 * 最多保存 [MAX_PROFILES] 套，可随时切换用哪个 AI 改卷。
 * 用槽位前缀存在同一份 SharedPreferences 里：
 * ```
 * profile_0_name / profile_0_key / profile_0_url / profile_0_model
 * profile_1_...
 * active_slot
 * ```
 * 不用 JSON：SharedPreferences 本身就是 key-value，存槽位更直观，
 * 也不会因为某套配置里有特殊字符而解析失败。
 *
 * ## 为什么覆盖安装不会丢
 *
 * SharedPreferences 位于 `/data/data/<包名>/shared_prefs/`，
 * 覆盖安装（同签名）保留应用数据目录，因此配置自然继承。
 * 只有「卸载重装」或「清除应用数据」才会丢。
 *
 * ## 从旧版单套配置迁移
 *
 * v1.3.1 及以前只存 `api_key` / `base_url` / `model` 三个键。
 * 首次读取时会自动搬进槽位 0，老用户升级后配置不丢。
 *
 * ## 为什么不用 EncryptedSharedPreferences
 *  1. 要求 API 23+，部分 Android 8 机型有已知兼容问题
 *  2. 会引入 androidx.security 依赖，只为存几个字符串不划算
 *  3. 这是用户自己的 API Key，位于应用私有目录已被沙箱保护
 */
class ApiConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences("ai_api_config", Context.MODE_PRIVATE)

    init {
        migrateLegacyIfNeeded()
    }

    // ==================== 读取 ====================

    /** 已保存的配置（按槽位升序，跳过空槽） */
    fun profiles(): List<AiProfile> =
        (0 until MAX_PROFILES)
            .map { slot -> readSlot(slot) }
            .filter { it != null }
            .map { it!! }

    /** 当前选中的槽位；没有选中或选中槽为空时返回 -1 */
    fun activeSlot(): Int {
        val slot = prefs.getInt(KEY_ACTIVE_SLOT, -1)
        return if (slot in 0 until MAX_PROFILES && readSlot(slot) != null) slot else -1
    }

    /** 当前用于评分的配置 */
    fun activeProfile(): AiProfile? {
        val slot = activeSlot()
        return if (slot >= 0) readSlot(slot) else profiles().firstOrNull()
    }

    /** 还能新增几套 */
    fun remainingSlots(): Int = MAX_PROFILES - profiles().size

    /** 第一个空槽；已满返回 -1 */
    fun firstEmptySlot(): Int =
        (0 until MAX_PROFILES).firstOrNull { readSlot(it) == null } ?: -1

    // ==================== 写入 ====================

    /**
     * 保存一套配置。
     *
     * @return true 表示成功；false 表示槽位非法或新增时已满
     */
    fun save(profile: AiProfile): Boolean {
        if (profile.slot !in 0 until MAX_PROFILES) return false
        // 新增（该槽原本为空）时检查容量
        if (readSlot(profile.slot) == null && profiles().size >= MAX_PROFILES) return false

        prefs.edit()
            .putString(key(profile.slot, "name"), profile.name.trim())
            .putString(key(profile.slot, "key"), profile.apiKey.trim())
            .putString(key(profile.slot, "url"), profile.baseUrl.trim().trimEnd('/'))
            .putString(key(profile.slot, "model"), profile.model.trim())
            .apply()
        return true
    }

    /** 切换当前使用的配置 */
    fun selectSlot(slot: Int) {
        if (slot in 0 until MAX_PROFILES && readSlot(slot) != null) {
            prefs.edit().putInt(KEY_ACTIVE_SLOT, slot).apply()
        }
    }

    /** 删除一套配置 */
    fun deleteSlot(slot: Int) {
        prefs.edit()
            .remove(key(slot, "name"))
            .remove(key(slot, "key"))
            .remove(key(slot, "url"))
            .remove(key(slot, "model"))
            .apply()

        // 删的正是当前使用的 → 自动切到剩下的第一套
        if (prefs.getInt(KEY_ACTIVE_SLOT, -1) == slot) {
            val next = profiles().firstOrNull()
            prefs.edit()
                .putInt(KEY_ACTIVE_SLOT, next?.slot ?: -1)
                .apply()
        }
    }

    // ==================== 兼容旧调用 ====================

    /** 当前配置的 API Key */
    val apiKey: String get() = activeProfile()?.apiKey.orEmpty()

    /** 当前配置的 Base URL */
    val baseUrl: String get() = activeProfile()?.baseUrl.orEmpty()

    /** 当前配置的模型名 */
    val model: String get() = activeProfile()?.model.orEmpty()

    /** 是否已完成必要配置 */
    val isConfigured: Boolean get() = activeProfile()?.isComplete == true

    /** 校验当前配置，缺失时给出明确提示（不静默失败） */
    fun validate(): String? {
        val p = activeProfile() ?: return "还没有配置任何 AI，请到设置页添加"
        return when {
            p.apiKey.isBlank() -> "未配置 API Key，请到设置页填写"
            p.baseUrl.isBlank() -> "未配置 Base URL，请到设置页填写"
            p.model.isBlank() -> "未配置模型名，请到设置页填写"
            !p.baseUrl.startsWith("http") -> "Base URL 需以 http:// 或 https:// 开头"
            else -> null
        }
    }

    // ==================== 内部 ====================

    private fun key(slot: Int, field: String) = "profile_${slot}_$field"

    private fun readSlot(slot: Int): AiProfile? {
        val apiKey = prefs.getString(key(slot, "key"), null)
        val baseUrl = prefs.getString(key(slot, "url"), null)
        val model = prefs.getString(key(slot, "model"), null)
        val name = prefs.getString(key(slot, "name"), "").orEmpty()

        // 三个必要字段全空视为空槽
        if (apiKey.isNullOrBlank() && baseUrl.isNullOrBlank() && model.isNullOrBlank()) {
            return null
        }
        return AiProfile(
            slot = slot,
            name = name,
            apiKey = apiKey.orEmpty(),
            baseUrl = baseUrl.orEmpty(),
            model = model.orEmpty(),
        )
    }

    /**
     * 把 v1.3.1 及以前的单套配置搬到槽位 0。
     *
     * 只在「旧键存在且槽位 0 为空」时执行，并且执行后打标记，
     * 避免用户清空槽位 0 后又被旧数据覆盖回来。
     */
    private fun migrateLegacyIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED_V1, false)) return

        val legacyKey = prefs.getString(LEGACY_KEY_API_KEY, null).orEmpty()
        val legacyUrl = prefs.getString(LEGACY_KEY_BASE_URL, null).orEmpty()
        val legacyModel = prefs.getString(LEGACY_KEY_MODEL, null).orEmpty()

        if (legacyKey.isNotBlank() || legacyUrl.isNotBlank() || legacyModel.isNotBlank()) {
            if (readSlot(0) == null) {
                prefs.edit()
                    .putString(key(0, "name"), "默认配置")
                    .putString(key(0, "key"), legacyKey)
                    .putString(key(0, "url"), legacyUrl.ifBlank { DEFAULT_BASE_URL })
                    .putString(key(0, "model"), legacyModel.ifBlank { DEFAULT_MODEL })
                    .putInt(KEY_ACTIVE_SLOT, 0)
                    .apply()
            }
        }

        // 旧键保留不删：万一迁移逻辑出问题，用户还能从旧键找回
        prefs.edit().putBoolean(KEY_MIGRATED_V1, true).apply()
    }

    companion object {
        /** 最多保存的配置套数 */
        const val MAX_PROFILES = 3

        private const val KEY_ACTIVE_SLOT = "active_slot"
        private const val KEY_MIGRATED_V1 = "migrated_to_profiles_v1"

        // v1.3.1 及以前的单套配置键
        private const val LEGACY_KEY_API_KEY = "api_key"
        private const val LEGACY_KEY_BASE_URL = "base_url"
        private const val LEGACY_KEY_MODEL = "model"

        /** 默认 DeepSeek（OpenAI 兼容协议） */
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}
