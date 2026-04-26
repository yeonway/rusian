package com.rusian.app.data.chat

import com.rusian.app.domain.model.ChatMessage
import com.rusian.app.domain.model.ChatRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class LlamaCppLocalModelRunner @Inject constructor() : LocalModelRunner {
    private val mutex = Mutex()
    private var loadedModelPath: String? = null
    private var nativeLlama: NativeLlama? = null

    override suspend fun generate(
        modelPath: String,
        history: List<ChatMessage>,
        userPrompt: String,
        maxTokens: Int,
        onProgress: (Float) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val runtime = ensureLoaded(modelPath)
            runtime.generate(
                prompt = buildPrompt(history, userPrompt),
                maxTokens = maxTokens,
                onProgress = onProgress,
            ).trim()
        }
    }

    override fun cancelGeneration() {
        nativeLlama?.cancel()
    }

    private fun ensureLoaded(modelPath: String): NativeLlama {
        val current = nativeLlama
        if (current != null && loadedModelPath == modelPath) {
            return current
        }

        current?.close()
        val next = NativeLlama()
        next.load(
            modelPath = modelPath,
            contextSize = 512,
            threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4),
        )
        nativeLlama = next
        loadedModelPath = modelPath
        return next
    }

    private fun buildPrompt(history: List<ChatMessage>, userPrompt: String): String {
        val recent = history.takeLast(2)
        return buildString {
            appendLine("너는 러시아어 학습 도우미다. 답변은 항상 한국어로 한다.")
            appendLine("답변은 1~5문장으로 짧게 한다. 필요할 때만 러시아어 예시 1개를 붙인다.")
            appendLine("러시아어는 예시나 교정에만 쓰고, 설명은 한국어로 한다.")
            appendLine()
            recent.forEach { message ->
                when (message.role) {
                    ChatRole.USER -> appendLine("사용자: ${message.text}")
                    ChatRole.ASSISTANT -> appendLine("도우미: ${message.text}")
                }
            }
            appendLine("사용자: $userPrompt")
            append("도우미:")
        }
    }
}
