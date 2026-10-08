package com.cetsix.score.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cetsix.core.data.repository.ExamRecordRepository
import com.cetsix.feature.aiassistant.AiAssistantScreen
import com.cetsix.feature.aiassistant.AiAssistantViewModel
import com.cetsix.feature.aiassistant.rememberClipboardCopier
import com.cetsix.feature.assessment.EstimateInputScreen
import com.cetsix.feature.assessment.EstimateInputViewModel
import com.cetsix.feature.history.HistoryScreen
import com.cetsix.feature.history.HistoryViewModel
import com.cetsix.feature.home.HomeScreen
import com.cetsix.feature.result.EstimateResultScreen
import com.cetsix.feature.result.ResultViewModel
import com.cetsix.feature.trend.TrendScreen
import com.cetsix.feature.trend.TrendViewModel
import com.cetsix.score.domain.model.ExamType
import com.cetsix.score.domain.model.ScoringTask
import dev.chrisbanes.haze.rememberHazeState
import com.cetsix.feature.aiassistant.pro.ProApiBridgeImpl
import com.cetsix.feature.aiassistant.pro.ProScoringPanel
import com.cetsix.score.domain.model.ScoreResult

/** 全局路由表 */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val TREND = "trend"
    const val ASSESSMENT = "assessment/{examType}"
    const val RESULT = "result/{examType}"
    const val AI_ASSISTANT =
        "ai_assistant?examType={examType}&taskType={taskType}&presetContext={presetContext}"

    /** 标记是否带预设上下文（决定从第一屏开始还是直达第三屏） */
    const val ARG_PRESET_CONTEXT = "presetContext"

    /**
     * 首页入口：用户从第一屏（级别选择）开始，完整走替换式导航。
     */
    const val AI_ASSISTANT_ENTRY = "ai_assistant?presetContext=false"

    /**
     * 估分页「用 AI 精评」入口：已选定级别与题型，直达第三屏评分界面。
     */
    fun aiAssistantWithContext(examType: ExamType, taskType: ScoringTask) =
        "ai_assistant?examType=${examType.name}&taskType=${taskType.name}&presetContext=true"

    fun assessment(type: ExamType) = "assessment/${type.name}"
    fun result(type: ExamType) = "result/${type.name}"
}

/**
 * 应用唯一的 NavHost。
 *
 * 估分页 → 结果页的数据传递：
 * 结果对象不适合塞进路由参数（体积大），所以用一个导航级的 [PendingResult]
 * 暂存，结果页进入时取走。暂存器随 NavHost 重组，仅存在于进程内存。
 */
@Composable
fun AppNavHost(
    repositoryProvider: () -> ExamRecordRepository,
    navController: NavHostController = rememberNavController(),
) {
    val pending = remember { PendingResult() }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onExamSelected = { navController.navigate(Routes.assessment(it)) },
                onHistoryClick = { navController.navigate(Routes.HISTORY) },
                onTrendClick = { navController.navigate(Routes.TREND) },
                onAiAssistantClick = {
                    // 从第一屏（级别选择）开始，不预设任何上下文
                    navController.navigate(Routes.AI_ASSISTANT_ENTRY)
                },
            )
        }

        // ---------- 估分输入页 ----------
        composable(
            route = Routes.ASSESSMENT,
            arguments = listOf(navArgument("examType") { type = NavType.StringType }),
        ) { entry ->
            val examType = ExamType.fromName(entry.arguments?.getString("examType"))
            val vm: EstimateInputViewModel = viewModel(
                factory = factoryOf { EstimateInputViewModel(examType) }
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            EstimateInputScreen(
                uiState = uiState,
                onAnswerChange = vm::onAnswerChange,
                onWritingChange = vm::onWritingScoreChange,
                onTranslationChange = vm::onTranslationScoreChange,
                onAiRefineWriting = {
                    navController.navigate(
                        Routes.aiAssistantWithContext(examType, ScoringTask.ESSAY)
                    )
                },
                onAiRefineTranslation = {
                    navController.navigate(
                        Routes.aiAssistantWithContext(examType, ScoringTask.TRANSLATION)
                    )
                },
                onCalculate = {
                    val result = vm.calculateNow()
                    if (result != null) {
                        pending.value = result to System.currentTimeMillis()
                        navController.navigate(Routes.result(examType))
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        // ---------- 结果页 ----------
        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument("examType") { type = NavType.StringType }),
        ) { entry ->
            val vm: ResultViewModel = viewModel(
                factory = factoryOf { ResultViewModel(repositoryProvider()) }
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            // 首次进入时注入结果；注入后 ViewModel 内部已持有，不会重复覆盖
            if (uiState.result == null) {
                pending.value?.let { (result, createdAt) ->
                    vm.setResult(result, createdAt)
                }
            }

            EstimateResultScreen(
                result = uiState.result,
                createdAt = uiState.createdAt,
                isSaved = uiState.isSaved,
                showNoteDialog = uiState.showNoteDialog,
                onShowNoteDialog = vm::onShowNoteDialog,
                onSave = vm::onSave,
                onRestart = {
                    pending.value = null
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }

        // ---------- AI 评分助手页 ----------
        composable(
            route = Routes.AI_ASSISTANT,
            arguments = listOf(
                navArgument("examType") {
                    type = NavType.StringType
                    defaultValue = ExamType.CET4.name
                },
                navArgument("taskType") {
                    type = NavType.StringType
                    defaultValue = ScoringTask.ESSAY.name
                },
                navArgument(Routes.ARG_PRESET_CONTEXT) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val vm: AiAssistantViewModel = viewModel()
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val copier = rememberClipboardCopier()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            // 专业模式桥接：整个 App 生命周期内单例，保持流式输出状态
            val proBridge = remember(context) { ProApiBridgeImpl(context, scope) }

            // ============================================================
            // 入口语义：
            //  · 首页「AI 评分助手」卡片 → 从**第一屏**（四级/六级）开始，
            //    让用户完整走一遍替换式导航。
            //  · 估分页的「用 AI 精评」按钮 → 已经带着 examType/taskType 上下文，
            //    直接跳到第三屏，省去重复选择。
            //
            // 用 presence 标记区分，而不是「有没有传参数」——
            // 因为首页入口也会带一组默认值参数进来。
            // ============================================================
            val hasPresetContext = entry.arguments?.getBoolean(
                Routes.ARG_PRESET_CONTEXT, false
            ) ?: false

            val hazeState = rememberHazeState()

            if (hasPresetContext) {
                val examType = ExamType.fromName(entry.arguments?.getString("examType"))
                val taskType = ScoringTask.entries.firstOrNull {
                    it.name == entry.arguments?.getString("taskType")
                } ?: ScoringTask.ESSAY
                LaunchedEffect(Unit) {
                    vm.jumpToScoring(examType, taskType)
                }
            }
            // 否则不动 —— ViewModel 默认就停在第一屏 LevelSelect

            AiAssistantScreen(
                hazeState = hazeState,
                uiState = uiState,
                onLevelSelected = vm::onLevelSelected,
                onTaskSelected = vm::onTaskSelected,
                onQuestionChange = vm::onQuestionChange,
                onAnswerChange = vm::onAnswerChange,
                onClearAnswer = vm::onClearAnswer,
                onModeChange = vm::onModeChange,
                onCopyPrompt = { text ->
                    copier(text)
                },
                onBack = {
                    // 返回：第三屏→第二屏→第一屏→关闭
                    if (!vm.onBack()) {
                        navController.popBackStack()
                    }
                },
                proSlot = {
                    // 专业模式面板：用户主动切到专业模式后才渲染
                    ProScoringPanel(
                        hazeState = hazeState,
                        bridge = proBridge,
                        prompt = uiState.prompt,
                        question = uiState.question,
                        answer = uiState.answer,
                        onQuestionChange = vm::onQuestionChange,
                        onAnswerChange = vm::onAnswerChange,
                    )
                },
            )
        }

        // ---------- 历史记录页 ----------
        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(
                factory = factoryOf { HistoryViewModel(repositoryProvider()) }
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            HistoryScreen(
                uiState = uiState,
                onFilterChange = vm::onFilterChange,
                onShowDetail = vm::onShowDetail,
                onDismissDetail = vm::onDismissDetail,
                onEditNote = vm::onEditNote,
                onDismissNoteDialog = vm::onDismissNoteDialog,
                onUpdateNote = vm::onUpdateNote,
                onDelete = vm::onDelete,
            )
        }

        // ---------- 趋势图页 ----------
        composable(Routes.TREND) {
            val vm: TrendViewModel = viewModel(
                factory = factoryOf { TrendViewModel(repositoryProvider()) }
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            TrendScreen(
                uiState = uiState,
                onFilterChange = vm::onFilterChange,
                onPointClick = vm::onPointClick,
                onCloseDetail = vm::onCloseDetail,
                onDeleteSelected = vm::onDeleteSelected,
            )
        }
    }
}

/** 估分结果暂存器（仅进程内存） */
class PendingResult {
    var value: Pair<ScoreResult, Long>? = null
}

/** 极简 ViewModel 工厂，避免为一两个参数引入额外依赖 */
private inline fun <reified VM : ViewModel> factoryOf(
    crossinline create: () -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}

/** 从 Application 取得 Repository */
@Composable
fun rememberRepository(): ExamRecordRepository {
    val app = LocalContext.current.applicationContext as Application
    return remember(app) { (app as com.cetsix.score.CetSixApp).repository }
}
