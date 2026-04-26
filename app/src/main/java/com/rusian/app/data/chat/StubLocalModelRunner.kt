package com.rusian.app.data.chat

import com.rusian.app.domain.model.ChatMessage
import com.rusian.app.domain.model.ChatRole
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class StubLocalModelRunner @Inject constructor() : LocalModelRunner {
    override suspend fun generate(
        modelPath: String,
        history: List<ChatMessage>,
        userPrompt: String,
        maxTokens: Int,
        onProgress: (Float) -> Unit,
    ): String {
        // Placeholder local runner. Replace with llama.cpp JNI call later.
        onProgress(0.25f)
        delay(220)
        onProgress(1f)
        val modelName = File(modelPath).name
        val lastAssistant = history.lastOrNull { it.role == ChatRole.ASSISTANT }?.text
        return buildString {
            append("[$modelName] ")
            append("로컬 모델 샘플 응답: ")
            append(userPrompt.trim())
            if (!lastAssistant.isNullOrBlank()) {
                append("\n이전 답변 이어서 참고: ")
                append(lastAssistant.take(80))
            }
        }
    }
}
