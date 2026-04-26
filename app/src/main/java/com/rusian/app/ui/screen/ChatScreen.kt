package com.rusian.app.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rusian.app.domain.model.ChatMessage
import com.rusian.app.domain.model.ChatRole
import com.rusian.app.domain.model.Conversation
import com.rusian.app.domain.model.ModelFile
import com.rusian.app.ui.component.AppLoadingView
import com.rusian.app.ui.component.ScreenContentPadding
import com.rusian.app.ui.viewmodel.ChatViewModel
import kotlin.math.roundToInt

private data class ChatColors(
    val background: Color,
    val backgroundMid: Color,
    val paneTop: Color,
    val paneMiddle: Color,
    val paneBottom: Color,
    val border: Color,
    val borderStrong: Color,
    val primary: Color,
    val secondary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val aiBubbleTop: Color,
    val aiBubbleBottom: Color,
    val row: Color,
    val rowSelected: Color,
)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    ChatScreenContent(viewModel = viewModel)
}

@Composable
private fun chatColors(): ChatColors {
    val scheme = MaterialTheme.colorScheme
    val light = scheme.background.luminance() > 0.5f
    return ChatColors(
        background = scheme.background,
        backgroundMid = if (light) scheme.surface else scheme.surfaceVariant.copy(alpha = 0.56f),
        paneTop = if (light) scheme.surface.copy(alpha = 0.98f) else scheme.surface.copy(alpha = 0.82f),
        paneMiddle = if (light) scheme.surfaceVariant.copy(alpha = 0.78f) else scheme.surfaceVariant.copy(alpha = 0.46f),
        paneBottom = if (light) scheme.surface.copy(alpha = 0.94f) else scheme.surface.copy(alpha = 0.66f),
        border = scheme.outlineVariant.copy(alpha = if (light) 0.72f else 0.38f),
        borderStrong = scheme.outline.copy(alpha = if (light) 0.36f else 0.30f),
        primary = scheme.primary,
        secondary = scheme.secondary,
        onPrimary = scheme.onPrimary,
        textPrimary = scheme.onBackground,
        textSecondary = scheme.onSurfaceVariant,
        textMuted = scheme.onSurfaceVariant.copy(alpha = 0.72f),
        aiBubbleTop = if (light) scheme.surfaceVariant.copy(alpha = 0.78f) else scheme.surfaceVariant.copy(alpha = 0.68f),
        aiBubbleBottom = if (light) scheme.surface.copy(alpha = 0.96f) else scheme.surface.copy(alpha = 0.58f),
        row = if (light) scheme.surface.copy(alpha = 0.82f) else scheme.surfaceVariant.copy(alpha = 0.34f),
        rowSelected = if (light) scheme.primaryContainer.copy(alpha = 0.52f) else scheme.primary.copy(alpha = 0.18f),
    )
}

@Composable
private fun ChatScreenContent(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = chatColors()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val selectedRoom = state.conversations.firstOrNull { it.id == state.selectedConversationId }
    val selectedModel = state.models.firstOrNull { it.id == selectedRoom?.modelId }
    val dimAlpha by animateFloatAsState(
        targetValue = if (sheetOpen) 0.56f else 0f,
        label = "chat-sheet-dim",
    )

    val pickModelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.importModelFromUri(uri)
        }
    }

    LaunchedEffect(state.messages.size, state.sending) {
        if (state.messages.isNotEmpty() || state.sending) {
            val targetIndex = if (state.sending) state.messages.size else state.messages.lastIndex
            listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
        }
    }

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            AppLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ScreenContentPadding),
                message = "채팅을 준비하는 중입니다",
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.background,
                        colors.backgroundMid,
                        colors.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MinimalTopBar(
                title = selectedRoom?.title ?: "로컬 채팅",
                subtitle = selectedModel?.name ?: "선택된 모델 없음",
                sheetOpen = sheetOpen,
                onToggleSheet = { sheetOpen = !sheetOpen },
            )

            MessageList(
                modifier = Modifier.weight(1f),
                messages = state.messages,
                selectedRoom = selectedRoom,
                listState = listState,
                aiTyping = state.sending,
            )

            state.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            if (!state.sending) state.statusMessage?.let {
                Text(
                    text = it,
                    color = colors.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
            state.generationProgress?.let { progress ->
                val speed = state.generationTokensPerSecond
                    ?.takeIf { it.isFinite() && it > 0f }
                    ?.let { " · ${((it * 10).roundToInt() / 10f)} tok/s" }
                    .orEmpty()
                Text(
                    text = "생성 ${(progress * 100).roundToInt()}%$speed",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }

            FloatingInput(
                value = state.messageInput,
                sending = state.sending,
                addingStudyData = state.addingStudyData,
                onValueChange = viewModel::onMessageInputChanged,
                onSend = viewModel::sendMessage,
                onCancel = viewModel::cancelSending,
            )
        }

        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
                    .clickable { sheetOpen = false },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ScreenContentPadding),
        ) {
            Spacer(modifier = Modifier.height(76.dp))
            AnimatedVisibility(
                visible = sheetOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ControlSheet(
                    models = state.models,
                    rooms = state.conversations,
                    selectedRoom = selectedRoom,
                    selectedModel = selectedModel,
                    roomNameInput = state.roomNameInput,
                    renameInput = state.renameInput,
                    importingModel = state.importingModel,
                    onPickModel = { pickModelLauncher.launch(arrayOf("*/*")) },
                    onSelectModel = viewModel::setModelForCurrentRoom,
                    onDeleteModel = viewModel::deleteModel,
                    onRoomNameChanged = viewModel::onRoomNameChanged,
                    onCreateRoom = viewModel::createRoom,
                    onSelectRoom = viewModel::selectConversation,
                    onRenameInputChanged = viewModel::onRenameInputChanged,
                    onRenameRoom = viewModel::renameCurrentRoom,
                    onDeleteRoom = viewModel::deleteCurrentRoom,
                )
            }
        }
    }
}

@Composable
private fun MinimalTopBar(
    title: String,
    subtitle: String,
    sheetOpen: Boolean,
    onToggleSheet: () -> Unit,
) {
    val colors = chatColors()
    GlassPane(
        modifier = Modifier.fillMaxWidth(),
        radius = 24.dp,
        padding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButtonChrome(
                text = if (sheetOpen) "닫기" else "관리",
                emphasized = sheetOpen,
                onClick = onToggleSheet,
            )
        }
    }
}

@Composable
private fun ControlSheet(
    models: List<ModelFile>,
    rooms: List<Conversation>,
    selectedRoom: Conversation?,
    selectedModel: ModelFile?,
    roomNameInput: String,
    renameInput: String,
    importingModel: Boolean,
    onPickModel: () -> Unit,
    onSelectModel: (Long) -> Unit,
    onDeleteModel: (Long) -> Unit,
    onRoomNameChanged: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onSelectRoom: (Long) -> Unit,
    onRenameInputChanged: (String) -> Unit,
    onRenameRoom: () -> Unit,
    onDeleteRoom: () -> Unit,
) {
    val colors = chatColors()
    GlassPane(
        modifier = Modifier.fillMaxWidth(),
        radius = 28.dp,
        padding = 16.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionLabel("모델")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = selectedModel?.name ?: "선택된 모델 없음",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButtonChrome(
                    text = if (importingModel) "가져오는 중" else "파일 선택",
                    emphasized = true,
                    enabled = !importingModel,
                    onClick = onPickModel,
                )
            }

            models.forEach { model ->
                ThinListRow(
                    title = model.name,
                    subtitle = "${toMb(model.sizeBytes)} MB",
                    selected = selectedModel?.id == model.id,
                    trailing = "삭제",
                    onClick = { onSelectModel(model.id) },
                    onTrailingClick = { onDeleteModel(model.id) },
                )
            }

            SectionLabel("채팅방")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinimalTextField(
                    value = roomNameInput,
                    onValueChange = onRoomNameChanged,
                    modifier = Modifier.weight(1f),
                    label = "새 채팅방",
                    singleLine = true,
                )
                TextButtonChrome(text = "생성", emphasized = true, onClick = onCreateRoom)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 168.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rooms, key = { it.id }) { room ->
                    ThinListRow(
                        modifier = Modifier.animateItem(),
                        title = room.title,
                        subtitle = if (room.id == selectedRoom?.id) "선택됨" else "",
                        selected = room.id == selectedRoom?.id,
                        trailing = null,
                        onClick = { onSelectRoom(room.id) },
                        onTrailingClick = null,
                    )
                }
            }

            if (selectedRoom != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MinimalTextField(
                        value = renameInput,
                        onValueChange = onRenameInputChanged,
                        modifier = Modifier.weight(1f),
                        label = "이름",
                        singleLine = true,
                    )
                    TextButtonChrome(text = "저장", emphasized = false, onClick = onRenameRoom)
                    TextButtonChrome(text = "삭제", emphasized = false, onClick = onDeleteRoom)
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    modifier: Modifier,
    messages: List<ChatMessage>,
    selectedRoom: Conversation?,
    listState: LazyListState,
    aiTyping: Boolean,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent),
    ) {
        if (selectedRoom == null) {
            val colors = chatColors()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "채팅방을 선택하세요",
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    modifier = Modifier.animateItem(),
                    message = message,
                )
            }
            if (aiTyping) {
                item(key = "ai-typing-indicator") {
                    TypingIndicatorBubble(modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun TypingIndicatorBubble(modifier: Modifier = Modifier) {
    val colors = chatColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.aiBubbleTop,
                            colors.aiBubbleBottom,
                        ),
                    ),
                )
                .border(
                    width = 0.5.dp,
                    color = colors.border,
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.textMuted.copy(alpha = 0.55f + index * 0.12f)),
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    modifier: Modifier = Modifier,
    message: ChatMessage,
) {
    val colors = chatColors()
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    brush = if (isUser) {
                        Brush.horizontalGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.92f),
                                colors.secondary.copy(alpha = 0.84f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                colors.aiBubbleTop,
                                colors.aiBubbleBottom,
                            ),
                        )
                    },
                )
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            colors.borderStrong,
                            colors.border,
                        ),
                    ),
                    shape = RoundedCornerShape(26.dp),
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isUser) 0.12f else 0.04f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            Text(
                text = message.text,
                color = if (isUser) colors.onPrimary else colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FloatingInput(
    value: String,
    sending: Boolean,
    addingStudyData: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    GlassPane(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        radius = 30.dp,
        padding = 10.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MinimalTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                label = "러시아어",
                singleLine = false,
            )
            if (sending) {
                TextButtonChrome(
                    text = "취소",
                    emphasized = true,
                    enabled = true,
                    onClick = onCancel,
                )
            } else {
                TextButtonChrome(
                    text = "전송",
                    emphasized = true,
                    enabled = true,
                    onClick = onSend,
                )
            }
        }
    }
}

@Composable
private fun GlassPane(
    modifier: Modifier = Modifier,
    radius: Dp,
    padding: Dp,
    content: @Composable () -> Unit,
) {
    val colors = chatColors()
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.paneTop,
                        colors.paneMiddle,
                        colors.paneBottom,
                    ),
                ),
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    listOf(colors.borderStrong, colors.border),
                ),
                shape = shape,
            )
            .padding(padding),
    ) {
        content()
    }
}

@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    label: String,
    singleLine: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        label = { Text(label) },
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            focusedBorderColor = scheme.primary.copy(alpha = 0.70f),
            unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.70f),
            focusedLabelColor = scheme.primary,
            unfocusedLabelColor = scheme.onSurfaceVariant,
            cursorColor = scheme.primary,
            focusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.34f),
            unfocusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.22f),
        ),
    )
}

@Composable
private fun ThinListRow(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    trailing: String?,
    onClick: () -> Unit,
    onTrailingClick: (() -> Unit)?,
) {
    val colors = chatColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.rowSelected else colors.row)
            .border(
                width = 0.5.dp,
                color = if (selected) colors.primary.copy(alpha = 0.52f) else colors.border,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = colors.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null && onTrailingClick != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = trailing,
                color = colors.textMuted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable(onClick = onTrailingClick),
            )
        }
    }
}

@Composable
private fun TextButtonChrome(
    text: String,
    emphasized: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = chatColors()
    val shape = RoundedCornerShape(18.dp)
    val background = if (emphasized) {
        Brush.horizontalGradient(
            listOf(
                colors.primary.copy(alpha = if (enabled) 0.92f else 0.28f),
                colors.secondary.copy(alpha = if (enabled) 0.84f else 0.24f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                colors.row.copy(alpha = if (enabled) 0.92f else 0.44f),
                colors.paneBottom.copy(alpha = if (enabled) 0.82f else 0.36f),
            ),
        )
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(
                width = 0.5.dp,
                color = if (emphasized) colors.borderStrong else colors.border,
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (emphasized) colors.onPrimary else colors.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = chatColors()
    Text(
        text = text,
        color = colors.textPrimary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun toMb(sizeBytes: Long): String {
    if (sizeBytes <= 0L) return "0.0"
    val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
    return String.format("%.1f", mb)
}
