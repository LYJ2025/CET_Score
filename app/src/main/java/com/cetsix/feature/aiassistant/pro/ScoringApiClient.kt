package com.cetsix.feature.aiassistant.pro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * AI 评分 API 客户端 —— **仅在 online flavor 编译**。
 *
 * 用 `HttpURLConnection` 而非 OkHttp/Retrofit：
 * 需求只要求「OpenAI 兼容的 SSE 流式接口」，
 * 原生 API 已够用，且离线版完全不受影响（这段代码不参与 offline 编译）。
 *
 * 流式协议（SSE）：
 * ```
 * data: {"choices":[{"delta":{"content":"我"}}]}
 * data: [DONE]
 * ```
 */
class ScoringApiClient(private val config: ApiConfigStore) {

    /** 请求超时上限（需求要求 60s） */
    private val timeoutMs = 60_000

    /**
     * 发起流式评分请求。
     *
     * @param prompt 完整提示词
     * @param onStage 阶段提示（如"正在准备评分上下文…"）
     * @param onDelta 每个增量文本
     * @param onDone 完成回调（成功带全文，失败带异常）
     */
    suspend fun streamScore(
        prompt: String,
        onStage: (String) -> Unit,
        onDelta: (String) -> Unit,
        onDone: (Result<String>) -> Unit,
    ) = withContext(Dispatchers.IO) {
        // 配置校验：缺失时明确提示，不静默失败
        config.validate()?.let { error ->
            onDone(Result.failure(ApiException.ConfigMissing(error)))
            return@withContext
        }

        val fullText = StringBuilder()
        var connection: HttpURLConnection? = null

        try {
            onStage("正在准备评分上下文…")

            connection = (URL("${config.baseUrl}/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = timeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            }

            // ---------- 组装请求体 ----------
            val body = buildRequestBody(config.model, prompt)
            connection.outputStream.use { it.write(body.toByteArray()) }

            // 阶段提示：连接已建立，等待首字节
            onStage("AI 正在阅读你的作答…")

            val code = connection.responseCode
            if (code !in 200..299) {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                onDone(Result.failure(ApiException.HttpError(code, err)))
                return@withContext
            }

            // ---------- 读取 SSE 流 ----------
            connection.inputStream.bufferedReader().use { reader ->
                reader.consumeSse { event ->
                    coroutineContext.ensureActive()   // 支持取消

                    // OpenAI 兼容的结束标记
                    if (event == "[DONE]") return@consumeSse false

                    // 解析 {"choices":[{"delta":{"content":"..."}}]}
                    val delta = SseParser.extractDelta(event)
                    if (delta != null && delta.isNotEmpty()) {
                        fullText.append(delta)
                        onDelta(delta)
                    }
                    true   // 继续读下一条
                }
            }

            if (fullText.isEmpty()) {
                onDone(
                    Result.failure(ApiException.ParseFailed("AI 未返回内容，请重试或换个模型试试")),
                )
            } else {
                onDone(Result.success(fullText.toString()))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            onDone(Result.failure(ApiException.NetworkError(e.message ?: "网络异常")))
        } finally {
            connection?.disconnect()
        }
    }

    /** 组装 OpenAI 兼容的请求体 */
    private fun buildRequestBody(model: String, prompt: String): String {
        val escaped = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        return """
            {
              "model": "$model",
              "stream": true,
              "temperature": 0.3,
              "messages": [
                { "role": "user", "content": "$escaped" }
              ]
            }
        """.trimIndent()
    }
}

/**
 * 逐行消费 SSE，直到 reader 结束或回调返回 false。
 */
private inline fun BufferedReader.consumeSse(onEvent: (String) -> Boolean) {
    while (true) {
        val line = readLine() ?: return
        // SSE 每条事件形如 `data: {...}`
        if (!line.startsWith("data:")) continue
        val payload = line.removePrefix("data:").trim()
        if (payload.isEmpty()) continue
        if (!onEvent(payload)) return
    }
}

/** SSE 解析工具 */
object SseParser {

    /**
     * 从一条 SSE 数据中提取增量文本。
     *
     * 不引 JSON 库，用轻量的字符串查找 —— 响应结构固定且简单，
     * 避免为一个字段引入整个序列化依赖。
     */
    fun extractDelta(payload: String): String? {
        val contentKey = "\"content\":"
        val idx = payload.indexOf(contentKey)
        if (idx < 0) return null

        var i = idx + contentKey.length
        // 跳过空白
        while (i < payload.length && payload[i].isWhitespace()) i++
        if (i >= payload.length || payload[i] != '"') return null

        // 手动解析字符串，处理转义
        i++
        val sb = StringBuilder()
        while (i < payload.length) {
            val c = payload[i]
            when {
                c == '\\' && i + 1 < payload.length -> {
                    when (val next = payload[i + 1]) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        else -> sb.append(next)
                    }
                    i += 2
                }
                c == '"' -> return sb.toString()   // 字符串结束
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    /**
     * 从 AI 输出中提取「最终得分」。
     *
     * 需求要求完成后高亮档位判定与最终分数，
     * 这里做一次宽松的模式匹配，找不到就返回 null（UI 照常展示原文）。
     */
    fun extractScore(text: String): String? {
        // 形如「最终得分：X / 15」或「最终得分: 11 / 15」
        val scoreRegex = Regex("最终得分\\s*[:：]\\s*(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+)")
        scoreRegex.find(text)?.let { m ->
            return "最终得分：${m.groupValues[1]} / ${m.groupValues[2]}"
        }

        // 形如「当前档位：11分档（10-12分）」
        val bandRegex = Regex("当前档位\\s*[:：]\\s*([^\n]+)")
        bandRegex.find(text)?.let { m ->
            return m.groupValues[1].trim()
        }

        return null
    }
}

/** API 相关异常 */
sealed class ApiException(message: String) : Exception(message) {
    /** 配置缺失 —— UI 应引导去设置页 */
    class ConfigMissing(message: String) : ApiException(message)
    /** HTTP 错误 */
    class HttpError(code: Int, val body: String) :
        ApiException("接口返回 $code${if (body.isNotBlank()) "：${body.take(200)}" else ""}")
    /** 网络异常 */
    class NetworkError(message: String) : ApiException(message)
    /** 解析失败 —— 原文照常展示 */
    class ParseFailed(message: String) : ApiException(message)
}
