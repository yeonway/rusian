package com.rusian.app.data.chat

import com.rusian.app.domain.model.ChatMessage

interface LocalModelRunner {
    suspend fun generate(
        modelPath: String,
        history: List<ChatMessage>,
        userPrompt: String,
        maxTokens: Int = 128,
        onProgress: (Float) -> Unit = {},
    ): String

    fun cancelGeneration() = Unit
}
