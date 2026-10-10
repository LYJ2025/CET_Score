package com.cetscore.score.domain.model

/**
 * AI 助手的**替换式导航**状态机 —— 纯逻辑，可单测。
 *
 * ============================ 什么是"替换式" ============================
 * 不是层层叠加，而是**同层级替换**：
 * ```
 * 第一屏：  四级 | 六级
 *            ↓ 点四级
 * 第二屏：  作文 | 翻译        ← 四级/六级按钮从这里消失
 *            ↓ 点作文
 * 第三屏：  四级-作文 评分界面
 * ```
 *
 * 禁止：
 *  - ❌ 第一屏和第二屏同时显示四级/六级按钮
 *  - ❌ 按钮层层叠加
 *  - ✅ 进一层 → 上一层按钮消失
 *  - ✅ 退一层 → 下一层按钮消失
 *
 * ============================ 返回逻辑 ============================
 *  - 第三屏返回 → 第二屏（题型选择），**级别信息保留**
 *  - 第二屏返回 → 第一屏（级别选择），**并清空已选级别**
 *
 * 关键设计：级别信息存在 [level] 里，即使回到第一屏也保留，
 * 这样从第三屏返回第二屏时能立刻恢复"四级-作文"的上下文。
 * 但**第二屏返回第一屏时按需求清空**，用 [backToLevelSelect] 显式重置。
 */
sealed interface AssistantStep {
    /** 第一屏：选择级别（四级 / 六级） */
    data object LevelSelect : AssistantStep

    /** 第二屏：选择题型（作文 / 翻译） */
    data object TaskSelect : AssistantStep

    /** 第三屏：评分界面 */
    data class Scoring(
        val examType: ExamType,
        val questionType: QuestionType,
    ) : AssistantStep
}

/**
 * 替换式导航的状态容器。
 *
 * 用法：
 * ```kotlin
 * val nav = AssistantNavigation()
 * nav.selectLevel(ExamType.CET4)        // 第一屏 → 第二屏
 * nav.currentStep                      // TaskSelect
 * nav.selectTask(QuestionType.WRITING)    // 第二屏 → 第三屏
 * nav.currentStep                      // Scoring(CET4, ESSAY)
 * nav.back()                           // → TaskSelect（级别保留）
 * nav.back()                           // → LevelSelect（级别清空）
 * ```
 */
class AssistantNavigation {

    /** 当前所处的屏 */
    var currentStep: AssistantStep = AssistantStep.LevelSelect
        private set

    /** 当前选中的级别；未选时为 null */
    var examType: ExamType? = null
        private set

    /** 当前选中的题型 */
    var questionType: QuestionType? = null
        private set

    /**
     * 第一屏点击级别 → 进入第二屏。
     *
     * 上一屏的四级/六级按钮随即消失（因为 currentStep 变了，
     * UI 只渲染当前屏的内容）。
     */
    fun selectLevel(type: ExamType) {
        examType = type
        questionType = null          // 换了级别，题型选择作废
        currentStep = AssistantStep.TaskSelect
    }

    /**
     * 第二屏点击题型 → 进入第三屏。
     *
     * 若尚未选级别，视为从外部直接进入（如首页入口），补设默认级别。
     */
    fun selectTask(type: QuestionType) {
        val level = examType ?: ExamType.CET4.also { examType = it }
        questionType = type
        currentStep = AssistantStep.Scoring(level, type)
    }

    /** 从首页等外部入口直接跳到第三屏 */
    fun jumpToScoring(level: ExamType, type: QuestionType) {
        examType = level
        questionType = type
        currentStep = AssistantStep.Scoring(level, type)
    }

    /**
     * 返回上一层。
     *
     * - 当前在第三屏 → 回到第二屏，**级别保留**
     * - 当前在第二屏 → 回到第一屏，**并清空已选级别**
     * - 当前在第一屏 → 返回 false，交给宿主处理（关闭页面）
     *
     * @return 是否消费了这次返回
     */
    fun back(): Boolean = when (currentStep) {
        is AssistantStep.Scoring -> {
            // 回第二屏，级别与题型都保留，用户可改题型
            currentStep = AssistantStep.TaskSelect
            true
        }

        AssistantStep.TaskSelect -> {
            // 回第一屏，按需求清空级别
            examType = null
            questionType = null
            currentStep = AssistantStep.LevelSelect
            true
        }

        AssistantStep.LevelSelect -> false   // 已在第一屏，交给宿主
    }

    /** 重置到初始态 */
    fun reset() {
        currentStep = AssistantStep.LevelSelect
        examType = null
        questionType = null
    }

    /** 当前是否是第一屏（宿主据此决定是否显示返回按钮） */
    val isFirstScreen: Boolean get() = currentStep is AssistantStep.LevelSelect
}