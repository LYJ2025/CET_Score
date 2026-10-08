package com.cetsix.core.ui

/**
 * 全 App 共用的文案常量。
 *
 * 放在 core:ui 而非各 feature 内部，避免同一个免责声明在多个模块重复定义、
 * 后续修改时漏改一处。
 */
object AppCopy {
    /**
     * 免责声明 —— 全 App 统一口径。
     *
     * 需求原文：「本 App 为个人学习用途，估分结果基于简化模型，与官方成绩可能存在偏差，仅供参考。」
     */
    const val DISCLAIMER = "本 App 为个人学习用途，估分结果基于简化模型，与官方成绩可能存在偏差，仅供参考。"

    /** 及格线提示 */
    const val PASS_HINT = "425 分以上即视为通过，仅供参考"
}
