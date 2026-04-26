package com.rusian.app.data.chat

internal class NativeLlama : AutoCloseable {
    private var handle: Long = 0L

    fun load(modelPath: String, contextSize: Int, threads: Int) {
        close()
        handle = nativeCreate(modelPath, contextSize, threads)
    }

    fun generate(
        prompt: String,
        maxTokens: Int = 256,
        temperature: Float = 0.7f,
        topK: Int = 40,
        topP: Float = 0.9f,
        onProgress: (Float) -> Unit = {},
    ): String {
        check(handle != 0L) { "Local model is not loaded." }
        return nativeGenerate(
            handle,
            prompt,
            maxTokens,
            temperature,
            topK,
            topP,
            LlamaProgressCallback(onProgress),
        )
    }

    fun cancel() {
        val current = handle
        if (current != 0L) {
            nativeCancel(current)
        }
    }

    override fun close() {
        val current = handle
        if (current != 0L) {
            handle = 0L
            nativeClose(current)
        }
    }

    private external fun nativeCreate(modelPath: String, contextSize: Int, threads: Int): Long

    private external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topK: Int,
        topP: Float,
        progressCallback: LlamaProgressCallback,
    ): String

    private external fun nativeClose(handle: Long)

    private external fun nativeCancel(handle: Long)

    companion object {
        init {
            System.loadLibrary("rusian_llama")
        }
    }
}

internal fun interface LlamaProgressCallback {
    fun onProgress(progress: Float)
}
