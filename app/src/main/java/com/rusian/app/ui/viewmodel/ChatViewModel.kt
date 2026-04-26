package com.rusian.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.rusian.app.core.work.AiGenerationWorkScheduler
import com.rusian.app.domain.repository.ChatRepository
import com.rusian.app.ui.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiGenerationWorkScheduler: AiGenerationWorkScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(loading = true))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var preferredConversationId: Long? = null
    private var messagesJob: Job? = null
    private var sendMessageJob: Job? = null
    private var activeAiWorkId: UUID? = null
    private var activeAiWorkJob: Job? = null

    init {
        viewModelScope.launch {
            chatRepository.observeModels().collectLatest { models ->
                _state.update { it.copy(models = models, loading = false) }
            }
        }
        viewModelScope.launch {
            chatRepository.observeLastConversationId().collectLatest { id ->
                preferredConversationId = id
                reconcileConversationSelection()
            }
        }
        viewModelScope.launch {
            chatRepository.observeConversations().collectLatest { rooms ->
                _state.update { current ->
                    current.copy(
                        conversations = rooms,
                        loading = false,
                    )
                }
                reconcileConversationSelection()
            }
        }
    }

    fun onRoomNameChanged(value: String) {
        _state.update { it.copy(roomNameInput = value, errorMessage = null, statusMessage = null) }
    }

    fun onRenameInputChanged(value: String) {
        _state.update { it.copy(renameInput = value, errorMessage = null, statusMessage = null) }
    }

    fun onMessageInputChanged(value: String) {
        _state.update { it.copy(messageInput = value, errorMessage = null) }
    }

    fun createRoom() {
        viewModelScope.launch {
            val title = _state.value.roomNameInput
            val result = chatRepository.createConversation(title)
            result.onSuccess { room ->
                selectConversation(room.id)
                _state.update {
                    it.copy(
                        roomNameInput = "",
                        renameInput = room.title,
                        statusMessage = "채팅방을 만들었습니다.",
                        errorMessage = null,
                    )
                }
            }.onFailure { t ->
                _state.update { it.copy(errorMessage = t.message ?: "채팅방 생성 실패") }
            }
        }
    }

    fun renameCurrentRoom() {
        viewModelScope.launch {
            val roomId = _state.value.selectedConversationId ?: return@launch
            val title = _state.value.renameInput
            chatRepository.renameConversation(roomId, title)
                .onSuccess {
                    _state.update { it.copy(statusMessage = "채팅방 이름을 변경했습니다.", errorMessage = null) }
                }
                .onFailure { t ->
                    _state.update { it.copy(errorMessage = t.message ?: "이름 변경 실패") }
                }
        }
    }

    fun deleteCurrentRoom() {
        viewModelScope.launch {
            val roomId = _state.value.selectedConversationId ?: return@launch
            chatRepository.deleteConversation(roomId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            selectedConversationId = null,
                            messages = emptyList(),
                            renameInput = "",
                            statusMessage = "채팅방을 삭제했습니다.",
                        )
                    }
                    reconcileConversationSelection()
                }
                .onFailure { t ->
                    _state.update { it.copy(errorMessage = t.message ?: "채팅방 삭제 실패") }
                }
        }
    }

    fun selectConversation(conversationId: Long) {
        val room = _state.value.conversations.firstOrNull { it.id == conversationId } ?: return
        _state.update {
            it.copy(
                selectedConversationId = conversationId,
                renameInput = room.title,
                statusMessage = null,
                errorMessage = null,
            )
        }
        preferredConversationId = conversationId
        viewModelScope.launch { chatRepository.setLastConversationId(conversationId) }
        observeMessages(conversationId)
    }

    fun setModelForCurrentRoom(modelId: Long) {
        viewModelScope.launch {
            val roomId = _state.value.selectedConversationId ?: return@launch
            chatRepository.setConversationModel(roomId, modelId)
                .onSuccess {
                    _state.update { it.copy(statusMessage = "모델을 선택했습니다.", errorMessage = null) }
                }
                .onFailure { t ->
                    _state.update { it.copy(errorMessage = t.message ?: "모델 선택 실패") }
                }
        }
    }

    fun importModelFromUri(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(importingModel = true, statusMessage = null, errorMessage = null) }
            chatRepository.importModel(uri)
                .onSuccess { model ->
                    val currentRoomId = _state.value.selectedConversationId
                    if (currentRoomId != null) {
                        chatRepository.setConversationModel(currentRoomId, model.id)
                    }
                    _state.update {
                        it.copy(
                            importingModel = false,
                            statusMessage = "${model.name} 가져오기 완료",
                        )
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            importingModel = false,
                            errorMessage = t.message ?: "모델 가져오기 실패",
                        )
                    }
                }
        }
    }

    fun deleteModel(modelId: Long) {
        viewModelScope.launch {
            chatRepository.deleteModel(modelId)
                .onSuccess {
                    _state.update { it.copy(statusMessage = "모델을 삭제했습니다.", errorMessage = null) }
                }
                .onFailure { t ->
                    _state.update { it.copy(errorMessage = t.message ?: "모델 삭제 실패") }
                }
        }
    }

    fun sendMessage() {
        if (_state.value.sending) return
        sendMessageJob = viewModelScope.launch {
            val roomId = _state.value.selectedConversationId
            if (roomId == null) {
                _state.update { it.copy(errorMessage = "먼저 채팅방을 선택하세요.") }
                return@launch
            }
            val text = _state.value.messageInput.trim()
            if (text.isBlank()) {
                _state.update { it.copy(errorMessage = "메시지를 입력하세요.") }
                return@launch
            }

            _state.update {
                it.copy(
                    sending = true,
                    addingStudyData = false,
                    generationProgress = null,
                    generationTokensPerSecond = null,
                    errorMessage = null,
                    statusMessage = "AI가 답변을 만들고 있습니다.",
                )
            }
            chatRepository.saveUserMessage(roomId, text)
                .onSuccess {
                    _state.update {
                        it.copy(
                            messageInput = "",
                            generationProgress = null,
                            generationTokensPerSecond = null,
                            errorMessage = null,
                            statusMessage = null,
                        )
                    }
                    val generationStartedAt = System.currentTimeMillis()
                    chatRepository.sendMessage(
                        conversationId = roomId,
                        text = text,
                        persistUserMessage = false,
                        onProgress = { progress ->
                            val elapsedSeconds = ((System.currentTimeMillis() - generationStartedAt) / 1000f)
                                .coerceAtLeast(0.1f)
                            val tokensPerSecond = progress.coerceIn(0f, 1f) * CHAT_MAX_TOKENS / elapsedSeconds
                            _state.update {
                                it.copy(
                                    generationProgress = progress,
                                    generationTokensPerSecond = tokensPerSecond,
                                )
                            }
                        },
                    ).onSuccess {
                        _state.update {
                            it.copy(
                                sending = false,
                                generationProgress = null,
                                generationTokensPerSecond = null,
                                statusMessage = "AI 답변이 완료됐습니다.",
                                errorMessage = null,
                            )
                        }
                    }.onFailure { t ->
                        _state.update {
                            it.copy(
                                sending = false,
                                generationProgress = null,
                                generationTokensPerSecond = null,
                                errorMessage = t.message ?: "AI 답변 생성 실패",
                            )
                        }
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            sending = false,
                            generationProgress = null,
                            generationTokensPerSecond = null,
                            errorMessage = t.message ?: "메시지 저장 실패",
                        )
                    }
                }
        }
    }

    fun generateStudyData() {
        if (_state.value.sending) return
        sendMessageJob = viewModelScope.launch {
            val roomId = _state.value.selectedConversationId
            if (roomId == null) {
                _state.update { it.copy(errorMessage = "먼저 채팅방을 선택하세요.") }
                return@launch
            }
            val request = _state.value.messageInput.trim()
            if (request.isBlank()) {
                _state.update { it.copy(errorMessage = "추가할 러시아어를 입력하세요.") }
                return@launch
            }

            _state.update {
                it.copy(
                    sending = true,
                    addingStudyData = true,
                    generationProgress = null,
                    generationTokensPerSecond = null,
                    errorMessage = null,
                    statusMessage = "학습 데이터 추가를 준비하고 있습니다.",
                )
            }
            chatRepository.saveUserMessage(roomId, request)
                .onSuccess {
                    val workId = aiGenerationWorkScheduler.enqueueStudyData(roomId, request)
                    activeAiWorkId = workId
                    observeAiWork(workId, addingStudyData = true)
                    _state.update {
                        it.copy(
                            messageInput = "",
                            generationProgress = null,
                            generationTokensPerSecond = null,
                            errorMessage = null,
                            statusMessage = "학습 데이터 추가를 백그라운드에서 시작했습니다.",
                        )
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(
                            sending = false,
                            addingStudyData = false,
                            generationProgress = null,
                            generationTokensPerSecond = null,
                            errorMessage = t.message ?: "학습 데이터 저장 실패",
                        )
                    }
                }
        }
    }

    fun cancelSending() {
        sendMessageJob?.cancel()
        sendMessageJob = null
        chatRepository.cancelGeneration()
        activeAiWorkId?.let(aiGenerationWorkScheduler::cancelWork)
        activeAiWorkId = null
        activeAiWorkJob?.cancel()
        activeAiWorkJob = null
        _state.update {
            it.copy(
                sending = false,
                addingStudyData = false,
                generationProgress = null,
                generationTokensPerSecond = null,
                statusMessage = "생성을 취소했습니다.",
                errorMessage = null,
            )
        }
    }

    private fun observeAiWork(workId: UUID, addingStudyData: Boolean) {
        activeAiWorkJob?.cancel()
        activeAiWorkJob = viewModelScope.launch {
            aiGenerationWorkScheduler.observeWork(workId).collectLatest { info ->
                if (activeAiWorkId != workId || info == null) return@collectLatest
                when (info.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.BLOCKED,
                    -> _state.update {
                        it.copy(
                            sending = true,
                            addingStudyData = addingStudyData,
                            errorMessage = null,
                        )
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        activeAiWorkId = null
                        _state.update {
                            it.copy(
                                sending = false,
                                addingStudyData = false,
                                generationProgress = null,
                                generationTokensPerSecond = null,
                                statusMessage = if (addingStudyData) {
                                    "학습 데이터 추가가 완료됐습니다."
                                } else {
                                    "AI 답변이 완료됐습니다."
                                },
                                errorMessage = null,
                            )
                        }
                        activeAiWorkJob?.cancel()
                        activeAiWorkJob = null
                    }

                    WorkInfo.State.FAILED -> {
                        activeAiWorkId = null
                        _state.update {
                            it.copy(
                                sending = false,
                                addingStudyData = false,
                                generationProgress = null,
                                generationTokensPerSecond = null,
                                errorMessage = if (addingStudyData) {
                                    "학습 데이터 추가 실패"
                                } else {
                                    "AI 답변 생성 실패"
                                },
                            )
                        }
                        activeAiWorkJob?.cancel()
                        activeAiWorkJob = null
                    }

                    WorkInfo.State.CANCELLED -> {
                        activeAiWorkId = null
                        _state.update {
                            it.copy(
                                sending = false,
                                addingStudyData = false,
                                generationProgress = null,
                                generationTokensPerSecond = null,
                                statusMessage = "생성을 취소했습니다.",
                                errorMessage = null,
                            )
                        }
                        activeAiWorkJob?.cancel()
                        activeAiWorkJob = null
                    }
                }
            }
        }
    }

    private fun reconcileConversationSelection() {
        val current = _state.value
        val rooms = current.conversations
        if (rooms.isEmpty()) {
            _state.update {
                it.copy(
                    selectedConversationId = null,
                    renameInput = "",
                    messages = emptyList(),
                )
            }
            messagesJob?.cancel()
            return
        }
        val selected = current.selectedConversationId
        val stillValid = rooms.any { it.id == selected }
        if (stillValid && selected != null) {
            return
        }
        val candidate = rooms.firstOrNull { it.id == preferredConversationId }?.id
            ?: rooms.first().id
        selectConversation(candidate)
    }

    private fun observeMessages(conversationId: Long) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collectLatest { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
    }

    private companion object {
        const val CHAT_MAX_TOKENS = 160
    }
}
