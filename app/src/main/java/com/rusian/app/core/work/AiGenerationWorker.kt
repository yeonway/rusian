package com.rusian.app.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rusian.app.domain.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class AiGenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val chatRepository: ChatRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val conversationId = inputData.getLong(KEY_CONVERSATION_ID, -1L)
        val promptFilePath = inputData.getString(KEY_PROMPT_FILE_PATH)
        val promptFile = promptFilePath?.let { File(it) }
        val prompt = if (promptFile != null && promptFile.exists()) {
            promptFile.readText()
        } else {
            inputData.getString(KEY_PROMPT).orEmpty()
        }
        val kind = inputData.getString(KEY_KIND).orEmpty()

        if (conversationId <= 0L || prompt.isBlank()) return Result.failure()

        val result = when (kind) {
            KIND_STUDY_DATA -> chatRepository.generateStudyData(
                conversationId = conversationId,
                request = prompt,
                persistUserMessage = false,
            )
            KIND_CHAT -> chatRepository.sendMessage(
                conversationId = conversationId,
                text = prompt,
                persistUserMessage = false,
            )
            else -> return Result.failure()
        }
        promptFile?.delete()
        return if (result.isSuccess) Result.success() else Result.failure()
    }

    companion object {
        const val KEY_CONVERSATION_ID = "conversation_id"
        const val KEY_PROMPT = "prompt"
        const val KEY_PROMPT_FILE_PATH = "prompt_file_path"
        const val KEY_KIND = "kind"
        const val KIND_CHAT = "chat"
        const val KIND_STUDY_DATA = "study_data"
    }
}
