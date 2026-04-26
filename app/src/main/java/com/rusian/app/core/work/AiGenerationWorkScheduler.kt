package com.rusian.app.core.work

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AiGenerationWorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueueChatMessage(conversationId: Long, prompt: String): UUID {
        return enqueue(conversationId, prompt, AiGenerationWorker.KIND_CHAT)
    }

    fun enqueueStudyData(conversationId: Long, prompt: String): UUID {
        return enqueue(conversationId, prompt, AiGenerationWorker.KIND_STUDY_DATA)
    }

    fun observeWork(id: UUID): Flow<WorkInfo?> {
        return WorkManager.getInstance(context).getWorkInfoByIdFlow(id)
    }

    fun cancelWork(id: UUID) {
        WorkManager.getInstance(context).cancelWorkById(id)
    }

    private fun enqueue(conversationId: Long, prompt: String, kind: String): UUID {
        val promptFile = writePromptFile(prompt)
        val request = OneTimeWorkRequestBuilder<AiGenerationWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(AiGenerationWorker.KEY_CONVERSATION_ID, conversationId)
                    .putString(AiGenerationWorker.KEY_PROMPT_FILE_PATH, promptFile.absolutePath)
                    .putString(AiGenerationWorker.KEY_KIND, kind)
                    .build(),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(request)
        return request.id
    }

    private fun writePromptFile(prompt: String): File {
        val dir = File(context.filesDir, "ai-generation-queue")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${UUID.randomUUID()}.txt")
        file.writeText(prompt)
        return file
    }
}
