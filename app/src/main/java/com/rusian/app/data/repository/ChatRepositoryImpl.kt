package com.rusian.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.rusian.app.data.chat.LocalModelRunner
import com.rusian.app.data.local.RusianDatabase
import com.rusian.app.data.local.dao.CategoryDao
import com.rusian.app.data.local.dao.ChatMessageDao
import com.rusian.app.data.local.dao.ConversationDao
import com.rusian.app.data.local.dao.ModelFileDao
import com.rusian.app.data.local.dao.StudyItemDao
import com.rusian.app.data.local.dao.WordDao
import com.rusian.app.data.local.dao.WordGlossDao
import com.rusian.app.data.local.entity.CategoryEntity
import com.rusian.app.data.local.entity.ChatMessageEntity
import com.rusian.app.data.local.entity.ConversationEntity
import com.rusian.app.data.local.entity.ModelFileEntity
import com.rusian.app.data.local.entity.StudyItemEntity
import com.rusian.app.data.local.entity.WordEntity
import com.rusian.app.data.local.entity.WordGlossEntity
import com.rusian.app.data.preferences.UserPreferencesGateway
import com.rusian.app.domain.model.ChatMessage
import com.rusian.app.domain.model.ChatRole
import com.rusian.app.domain.model.Conversation
import com.rusian.app.domain.model.ModelFile
import com.rusian.app.domain.model.StudyKind
import com.rusian.app.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: RusianDatabase,
    private val categoryDao: CategoryDao,
    private val modelFileDao: ModelFileDao,
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao,
    private val wordDao: WordDao,
    private val wordGlossDao: WordGlossDao,
    private val studyItemDao: StudyItemDao,
    private val preferencesGateway: UserPreferencesGateway,
    private val localModelRunner: LocalModelRunner,
    private val json: Json,
) : ChatRepository {

    override fun observeModels(): Flow<List<ModelFile>> {
        return modelFileDao.observeAll().map { list -> list.map { it.toModelFile() } }
    }

    override fun observeConversations(): Flow<List<Conversation>> {
        return conversationDao.observeAll().map { list -> list.map { it.toConversation() } }
    }

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.observeByConversationId(conversationId).map { list ->
            list.map { it.toChatMessage() }
        }
    }

    override fun observeLastConversationId(): Flow<Long?> {
        return preferencesGateway.preferences.map { it.lastChatConversationId }
    }

    override suspend fun importModel(uri: Uri): Result<ModelFile> = runCatching {
        withContext(Dispatchers.IO) {
            val displayName = resolveDisplayName(uri)
            require(displayName.lowercase().endsWith(".gguf")) {
                "Only .gguf files are supported."
            }

            val modelDir = File(context.filesDir, "models")
            if (!modelDir.exists()) modelDir.mkdirs()

            val safeName = sanitizeFileName(displayName)
            val target = File(modelDir, "${System.currentTimeMillis()}_$safeName")

            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open selected file." }
                target.outputStream().use { output -> input.copyTo(output) }
            }

            val now = System.currentTimeMillis()
            val entity = ModelFileEntity(
                name = safeName,
                filePath = target.absolutePath,
                fileSize = target.length(),
                createdAt = now,
            )
            val id = modelFileDao.insert(entity)
            entity.copy(id = id).toModelFile()
        }
    }

    override suspend fun deleteModel(modelId: Long): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val model = modelFileDao.getById(modelId) ?: return@withContext
            runCatching { File(model.filePath).delete() }
            modelFileDao.delete(model)
        }
    }

    override suspend fun createConversation(title: String): Result<Conversation> = runCatching {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val latestModel = modelFileDao.getLatest()
            val resolvedTitle = title.trim().ifBlank { "새 채팅" }
            val entity = ConversationEntity(
                title = resolvedTitle,
                modelId = latestModel?.id,
                createdAt = now,
                updatedAt = now,
            )
            val id = conversationDao.insert(entity)
            preferencesGateway.setLastChatConversationId(id)
            entity.copy(id = id).toConversation()
        }
    }

    override suspend fun renameConversation(conversationId: Long, title: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val resolvedTitle = title.trim().ifBlank { "새 채팅" }
            conversationDao.updateTitle(
                id = conversationId,
                title = resolvedTitle,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun deleteConversation(conversationId: Long): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            conversationDao.deleteById(conversationId)
            val latest = conversationDao.getLatest()
            preferencesGateway.setLastChatConversationId(latest?.id)
        }
    }

    override suspend fun setConversationModel(conversationId: Long, modelId: Long?): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            if (modelId != null) {
                requireNotNull(modelFileDao.getById(modelId)) { "Selected model does not exist." }
            }
            conversationDao.updateModel(
                id = conversationId,
                modelId = modelId,
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun setLastConversationId(conversationId: Long?) {
        preferencesGateway.setLastChatConversationId(conversationId)
    }

    override suspend fun saveUserMessage(conversationId: Long, text: String): Result<ChatMessage> = runCatching {
        withContext(Dispatchers.IO) {
            val prompt = text.trim()
            require(prompt.isNotBlank()) { "Message is empty." }
            requireNotNull(conversationDao.getById(conversationId)) {
                "Conversation not found."
            }
            insertUserMessage(conversationId = conversationId, text = prompt)
        }
    }

    override suspend fun sendMessage(
        conversationId: Long,
        text: String,
        persistUserMessage: Boolean,
        onProgress: (Float) -> Unit,
    ): Result<ChatMessage> = runCatching {
        withContext(Dispatchers.IO) {
            val prompt = text.trim()
            require(prompt.isNotBlank()) { "Message is empty." }

            val conversation = requireNotNull(conversationDao.getById(conversationId)) {
                "Conversation not found."
            }

            val modelId = requireNotNull(conversation.modelId) {
                "Choose a .gguf model for this chat room first."
            }
            val model = requireNotNull(modelFileDao.getById(modelId)) {
                "Selected model was removed."
            }
            val modelFile = File(model.filePath)
            require(modelFile.exists()) {
                "Model file is missing. Re-import the .gguf file."
            }

            val history = chatMessageDao.getRecent(conversationId, 4)
                .asReversed()
                .map { it.toChatMessage() }
                .dropLastCurrentUserMessage(prompt, enabled = !persistUserMessage)
            if (persistUserMessage) {
                insertUserMessage(conversationId = conversationId, text = prompt)
            }
            val answer = localModelRunner.generate(
                modelPath = model.filePath,
                history = history,
                userPrompt = prompt,
                maxTokens = CHAT_MAX_TOKENS,
                onProgress = onProgress,
            ).trim().ifBlank { "No response from local model." }

            val assistantCreatedAt = System.currentTimeMillis()
            val assistantId = chatMessageDao.insert(
                ChatMessageEntity(
                    conversationId = conversationId,
                    role = ChatRole.ASSISTANT.name,
                    text = answer,
                    createdAt = assistantCreatedAt,
                ),
            )
            conversationDao.touchUpdatedAt(conversationId, assistantCreatedAt)
            ChatMessage(
                id = assistantId,
                conversationId = conversationId,
                role = ChatRole.ASSISTANT,
                text = answer,
                createdAt = assistantCreatedAt,
            )
        }
    }

    override suspend fun generateStudyData(
        conversationId: Long,
        request: String,
        persistUserMessage: Boolean,
        onProgress: (Float) -> Unit,
    ): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            val userRequest = request.trim()
            require(userRequest.isNotBlank()) { "Study data request is empty." }

            val conversation = requireNotNull(conversationDao.getById(conversationId)) {
                "Conversation not found."
            }
            val modelId = requireNotNull(conversation.modelId) {
                "Choose a .gguf model for this chat room first."
            }
            val model = requireNotNull(modelFileDao.getById(modelId)) {
                "Selected model was removed."
            }
            val modelFile = File(model.filePath)
            require(modelFile.exists()) {
                "Model file is missing. Re-import the .gguf file."
            }

            val history = chatMessageDao.getRecent(conversationId, 8)
                .asReversed()
                .map { it.toChatMessage() }
                .dropLastCurrentUserMessage(userRequest, enabled = !persistUserMessage)
            if (persistUserMessage) {
                insertUserMessage(conversationId = conversationId, text = userRequest)
            }
            val batches = splitStudyDataRequest(userRequest)
            val answers = mutableListOf<String>()
            var importedCount = 0
            batches.forEachIndexed { index, batchRequest ->
                val categories = categoryDao.getAll()
                val categoriesText = categories.joinToString(separator = "\n") {
                    "- ${it.id}: ${it.name}"
                }
                val dataPrompt = buildStudyDataPrompt(
                    request = batchRequest,
                    categoriesText = categoriesText.ifBlank { "- ai-generated: AI 추가" },
                )
                val batchAnswer = localModelRunner.generate(
                    modelPath = model.filePath,
                    history = history,
                    userPrompt = dataPrompt,
                    maxTokens = STUDY_DATA_MAX_TOKENS,
                    onProgress = { progress ->
                        val batchBase = index.toFloat() / batches.size.toFloat()
                        val batchSize = 1f / batches.size.toFloat()
                        onProgress(batchBase + progress.coerceIn(0f, 1f) * batchSize)
                    },
                ).trim()
                answers += batchAnswer
                importedCount += importGeneratedStudyWords(batchAnswer)
            }

            val assistantCreatedAt = System.currentTimeMillis()
            chatMessageDao.insert(
                ChatMessageEntity(
                    conversationId = conversationId,
                    role = ChatRole.ASSISTANT.name,
                    text = buildString {
                        append(answers.joinToString(separator = "\n\n").ifBlank { "[]" })
                        append("\n\n앱 학습 데이터에 ")
                        append(importedCount)
                        append("개를 저장했습니다.")
                    },
                    createdAt = assistantCreatedAt,
                ),
            )
            conversationDao.touchUpdatedAt(conversationId, assistantCreatedAt)
            importedCount
        }
    }

    private suspend fun insertUserMessage(conversationId: Long, text: String): ChatMessage {
        val createdAt = System.currentTimeMillis()
        val id = chatMessageDao.insert(
            ChatMessageEntity(
                conversationId = conversationId,
                role = ChatRole.USER.name,
                text = text,
                createdAt = createdAt,
            ),
        )
        conversationDao.touchUpdatedAt(conversationId, createdAt)
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = ChatRole.USER,
            text = text,
            createdAt = createdAt,
        )
    }

    private fun List<ChatMessage>.dropLastCurrentUserMessage(
        prompt: String,
        enabled: Boolean,
    ): List<ChatMessage> {
        if (!enabled) return this
        val last = lastOrNull() ?: return this
        return if (last.role == ChatRole.USER && last.text.trim() == prompt) {
            dropLast(1)
        } else {
            this
        }
    }

    private fun splitStudyDataRequest(request: String): List<String> {
        val blocks = parseKoreanRussianBlocks(request)
        if (blocks.size <= STUDY_DATA_BATCH_SIZE) return listOf(request)
        return blocks.chunked(STUDY_DATA_BATCH_SIZE).map { batch ->
            batch.joinToString(separator = "\n\n") { block ->
                buildString {
                    append("러시아어: ")
                    append(block.russian)
                    appendLine()
                    append("한국어: ")
                    append(block.korean.orEmpty())
                }
            }
        }
    }

    private fun parseKoreanRussianBlocks(request: String): List<StudyInputBlock> {
        val pattern = Regex(
            pattern = """(?is)러시아어\s*:\s*(.*?)\s*한국어\s*:\s*(.*?)(?=\s*러시아어\s*:|$)""",
        )
        return pattern.findAll(request).mapNotNull { match ->
            val russian = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val korean = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
            russian.takeIf { it.isNotBlank() }?.let {
                StudyInputBlock(russian = it, korean = korean)
            }
        }.toList()
    }

    override fun cancelGeneration() {
        localModelRunner.cancelGeneration()
    }

    private fun buildStudyDataPrompt(request: String, categoriesText: String): String {
        return """
            사용자의 요청을 러시아어 학습 앱에 저장할 단어 JSON으로 변환하세요.
            설명 문장 없이 JSON만 출력하세요.
            출력 형식은 JSON 배열입니다.
            사용자 입력은 요청문이 아니라 러시아어 원문 목록일 수 있습니다.
            사용자가 "러시아어:"와 "한국어:" 양식으로 입력하면 러시아어 값은 word, 한국어 값은 meanings에 우선 반영하세요.
            "러시아어:"와 "한국어:" 블록이 여러 개 있으면 각 블록을 별도의 학습 항목으로 생성하세요.
            예: "러시아어: молоко / 한국어: 우유"와 "러시아어: чай / 한국어: 차"는 JSON 배열 항목 2개여야 합니다.
            "한국어:" 값이 비어 있으면 한국어 뜻을 직접 추론해서 meanings를 채우세요.
            입력 전체가 러시아어 단어/표현/문장뿐이면 각 줄, 쉼표, 글머리표 항목을 각각 학습 항목으로 처리하세요.
            사용자가 러시아어 단어/문장만 적었거나 일부 필드만 제공했으면, 빠진 필드는 직접 추론해서 채우세요.
            사용자가 개수를 적지 않고 러시아어 목록만 제공했으면, 제공된 러시아어 항목 수만큼 생성하세요.
            러시아어 한 줄이 문장이어도 word에는 그 표현을 그대로 넣고, 품사는 phrase로 둘 수 있습니다.
            categoryId와 categoryName도 비워두지 마세요.
            카테고리를 사용자가 지정하지 않았다면 기존 카테고리 중 가장 가까운 것을 고르세요.
            기존 카테고리에 맞지 않는 주제라면 새 categoryId는 영어 소문자, 숫자, 하이픈으로 만들고 categoryName은 한국어로 만드세요.
            한국어 뜻, 한글 발음, 로마자 표기, 품사, 예문과 해석도 비워두지 마세요.
            각 항목 필드:
            word, meanings, categoryId, categoryName, stress, transliteration, pronunciationKo,
            partOfSpeech, tone, situationHint, usageNote, contextTag, difficulty, exampleRu, exampleKo.
            meanings는 한국어 뜻 문자열 배열입니다.
            tone은 FORMAL, NEUTRAL, CASUAL, PLAYFUL, SLANG 중 하나입니다.
            difficulty는 1부터 5 사이 정수입니다.
            기존 categoryId를 우선 사용하세요.
            기존 카테고리:
            $categoriesText

            사용자 요청:
            $request
        """.trimIndent()
    }

    private suspend fun importGeneratedStudyWords(raw: String): Int {
        val generatedWords = parseGeneratedWords(raw)
            .filter { it.word.isNotBlank() && it.meanings.isNotEmpty() }
        require(generatedWords.isNotEmpty()) {
            "AI 응답에서 저장할 단어 JSON을 찾지 못했습니다."
        }

        database.withTransaction {
            val existingCategories = categoryDao.getAll()
            val knownCategoryIds = existingCategories.map { it.id }.toMutableSet()
            var nextSortOrder = (existingCategories.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val now = System.currentTimeMillis()

            generatedWords.forEach { generated ->
                val categoryId = normalizeCategoryId(generated.categoryId, generated.categoryName)
                if (knownCategoryIds.add(categoryId)) {
                    categoryDao.upsertAll(
                        listOf(
                            CategoryEntity(
                                id = categoryId,
                                name = generated.categoryName?.takeIf { it.isNotBlank() } ?: "AI 추가",
                                description = "AI가 앱 안에서 추가한 학습 데이터",
                                sortOrder = nextSortOrder++,
                            ),
                        ),
                    )
                }

                val existingWord = wordDao.findActiveByWord(generated.word)
                val wordId = existingWord?.id ?: generateWordId(generated.word)
                wordDao.upsertAll(
                    listOf(
                        WordEntity(
                            id = wordId,
                            categoryId = categoryId,
                            word = generated.word,
                            stress = generated.stress,
                            transliteration = generated.transliteration,
                            pronunciationKo = generated.pronunciationKo,
                            partOfSpeech = generated.partOfSpeech,
                            tone = normalizeTone(generated.tone),
                            situationHint = generated.situationHint,
                            usageNote = generated.usageNote,
                            pairKey = null,
                            contextTag = generated.contextTag,
                            difficulty = generated.difficulty.coerceIn(1, 5),
                            exampleRu = generated.exampleRu,
                            exampleKo = generated.exampleKo,
                            active = true,
                        ),
                    ),
                )

                wordGlossDao.deleteByWordId(wordId)
                wordGlossDao.upsertAll(
                    generated.meanings.mapIndexed { index, meaning ->
                        WordGlossEntity(
                            wordId = wordId,
                            locale = "ko",
                            meaningOrder = index,
                            meaning = meaning,
                        )
                    },
                )
                upsertGeneratedStudyItem(wordId = wordId, now = now)
            }
        }
        return generatedWords.size
    }

    private suspend fun upsertGeneratedStudyItem(wordId: String, now: Long) {
        val stableId = "word:$wordId"
        val existing = studyItemDao.findByRemoteStableId(stableId)
        if (existing == null) {
            studyItemDao.insert(
                StudyItemEntity(
                    kind = StudyKind.WORD,
                    remoteStableId = stableId,
                    alphabetId = null,
                    wordId = wordId,
                    isVisible = true,
                    createdAt = now,
                ),
            )
            return
        }
        studyItemDao.update(
            existing.copy(
                kind = StudyKind.WORD,
                alphabetId = null,
                wordId = wordId,
                isVisible = true,
            ),
        )
    }

    private fun parseGeneratedWords(raw: String): List<GeneratedStudyWord> {
        val candidate = extractJsonCandidate(raw)
        val element = json.parseToJsonElement(candidate)
        val items = when (element) {
            is JsonArray -> element
            is JsonObject -> {
                val words = element["words"]
                if (words is JsonArray) words else JsonArray(listOf(element))
            }
            else -> JsonArray(emptyList())
        }
        return items.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val example = obj["example"] as? JsonObject
            val meanings = (obj.stringList("meanings") + obj.stringList("meaning"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            GeneratedStudyWord(
                word = obj.stringValue("word").orEmpty(),
                meanings = meanings,
                categoryId = obj.stringValue("categoryId"),
                categoryName = obj.stringValue("categoryName"),
                stress = obj.stringValue("stress"),
                transliteration = obj.stringValue("transliteration"),
                pronunciationKo = obj.stringValue("pronunciationKo"),
                partOfSpeech = obj.stringValue("partOfSpeech"),
                tone = obj.stringValue("tone"),
                situationHint = obj.stringValue("situationHint"),
                usageNote = obj.stringValue("usageNote"),
                contextTag = obj.stringValue("contextTag"),
                difficulty = obj.intValue("difficulty") ?: 1,
                exampleRu = obj.stringValue("exampleRu") ?: example?.stringValue("ru"),
                exampleKo = obj.stringValue("exampleKo") ?: example?.stringValue("ko"),
            )
        }
    }

    private fun extractJsonCandidate(raw: String): String {
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        if (!fenced.isNullOrBlank()) return fenced

        val trimmed = raw.trim()
        val arrayStart = trimmed.indexOf('[')
        val arrayEnd = trimmed.lastIndexOf(']')
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1)
        }

        val objectStart = trimmed.indexOf('{')
        val objectEnd = trimmed.lastIndexOf('}')
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1)
        }
        return trimmed
    }

    private fun JsonObject.stringValue(name: String): String? {
        return (this[name] as? JsonPrimitive)
            ?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.stringList(name: String): List<String> {
        return when (val value = this[name]) {
            is JsonArray -> value.mapNotNull { item ->
                item.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
            }
            is JsonPrimitive -> listOfNotNull(value.contentOrNull?.trim()?.takeIf { it.isNotBlank() })
            else -> emptyList()
        }
    }

    private fun JsonObject.intValue(name: String): Int? {
        return (this[name] as? JsonPrimitive)?.intOrNull
    }

    private fun normalizeCategoryId(categoryId: String?, categoryName: String?): String {
        val explicit = categoryId?.trim()?.lowercase()
            ?.replace(Regex("[^a-z0-9_-]+"), "-")
            ?.trim('-')
            ?.takeIf { it.isNotBlank() }
        if (explicit != null) return explicit
        val name = categoryName?.trim().orEmpty()
        if (name.isBlank()) return "ai-generated"
        return "ai-cat-${sha256(name.lowercase()).take(10)}"
    }

    private fun normalizeTone(tone: String?): String {
        val normalized = tone?.trim()?.uppercase()
        return normalized?.takeIf {
            it in setOf("FORMAL", "NEUTRAL", "CASUAL", "PLAYFUL", "SLANG")
        } ?: "NEUTRAL"
    }

    private fun generateWordId(word: String): String {
        return "ai-word-${sha256(word.trim().lowercase()).take(16)}"
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun resolveDisplayName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                val value = it.getString(index)
                if (!value.isNullOrBlank()) return value
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "model.gguf"
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return cleaned.ifBlank { "model.gguf" }
    }

    private fun ModelFileEntity.toModelFile(): ModelFile {
        return ModelFile(
            id = id,
            name = name,
            path = filePath,
            sizeBytes = fileSize,
            createdAt = createdAt,
        )
    }

    private fun ConversationEntity.toConversation(): Conversation {
        return Conversation(
            id = id,
            title = title,
            modelId = modelId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun ChatMessageEntity.toChatMessage(): ChatMessage {
        val parsedRole = ChatRole.entries.firstOrNull { it.name == role } ?: ChatRole.USER
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = parsedRole,
            text = text,
            createdAt = createdAt,
        )
    }

    private data class GeneratedStudyWord(
        val word: String,
        val meanings: List<String>,
        val categoryId: String?,
        val categoryName: String?,
        val stress: String?,
        val transliteration: String?,
        val pronunciationKo: String?,
        val partOfSpeech: String?,
        val tone: String?,
        val situationHint: String?,
        val usageNote: String?,
        val contextTag: String?,
        val difficulty: Int,
        val exampleRu: String?,
        val exampleKo: String?,
    )

    private data class StudyInputBlock(
        val russian: String,
        val korean: String?,
    )

    private companion object {
        private const val STUDY_DATA_BATCH_SIZE = 20
        private const val CHAT_MAX_TOKENS = 160
        private const val STUDY_DATA_MAX_TOKENS = 320
    }
}
