package com.rusian.app.ui.screen

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.StudyCard
import com.rusian.app.domain.model.StudyKind
import com.rusian.app.ui.component.AppLoadingView
import com.rusian.app.ui.component.AppScreenContainer
import com.rusian.app.ui.component.EmptyStateCard
import com.rusian.app.ui.component.MetricChip
import com.rusian.app.ui.component.ScreenContentPadding
import com.rusian.app.ui.component.SectionHeader
import com.rusian.app.ui.component.SoftCard
import com.rusian.app.ui.component.rememberIsDenseHeight
import com.rusian.app.ui.viewmodel.StudyViewModel
import java.util.Locale
import kotlin.random.Random

private enum class AlphabetBoardFilter {
    ALL,
    PRONUNCIATION,
    WEAK,
    VOWEL,
    CONSONANT,
    SIGN,
}

private enum class AlphabetQuizDirection {
    KO_TO_RU,
    RU_TO_KO,
}

private data class AlphabetQuizQuestion(
    val card: StudyCard,
    val direction: AlphabetQuizDirection,
)

private val AlphabetBlue = Color(0xFF2F6BFF)
private val AlphabetBlueLight = Color(0xFFEAF0FF)
private val AlphabetBlueMid = Color(0xFFD8E5FF)
private val AlphabetBlueText = Color(0xFF1843B8)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun StudyScreen(
    title: String,
    mode: StudyKind,
    viewModel: StudyViewModel,
    quizStyle: Boolean,
    alphabetQuizOptionCount: Int = 4,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dense = rememberIsDenseHeight()
    val spacing = if (dense) 4.dp else 6.dp
    val alphabetCards = state.cards.filter { it.kind == StudyKind.ALPHABET }
    val confusingAlphabetCards = alphabetCards.filter {
        isConfusingAlphabetCard(it) || state.weakLetterIds.contains(it.remoteStableId)
    }
    var alphabetBoardFilter by rememberSaveable(mode) { mutableStateOf(AlphabetBoardFilter.ALL) }
    var alphabetFilterExpanded by rememberSaveable(mode) { mutableStateOf(false) }

    LaunchedEffect(mode) {
        viewModel.load(mode)
    }

    if (state.loading) {
        AppScreenContainer {
            AppLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ScreenContentPadding),
                message = "카드를 불러오는 중입니다",
            )
        }
        return
    }

    val card = state.currentCard
    val ratioTarget = if (state.cards.isEmpty()) {
        0f
    } else if (mode == StudyKind.ALPHABET) {
        1f
    } else {
        (state.currentIndex + 1).toFloat() / state.cards.size.toFloat()
    }
    val progressRatio by animateFloatAsState(
        targetValue = ratioTarget.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 260f),
        label = "study-progress",
    )
    val accuracy = if (state.sessionAnswered == 0) 0 else (state.sessionCorrect * 100 / state.sessionAnswered)

    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            if (mode != StudyKind.ALPHABET) {
                SectionHeader(title = title)
                SoftCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${state.currentIndex + 1} / ${state.cards.size.coerceAtLeast(1)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${accuracy}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (dense) 4.dp else 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MetricChip(text = "연속 ${state.currentStreak}")
                        MetricChip(text = "최고 ${state.bestStreak}")
                    }
                }
            }

            if (card == null) {
                EmptyStateCard(
                    modifier = Modifier.weight(1f),
                    title = "학습할 카드가 없습니다",
                    description = state.message ?: "오늘 분량을 완료했습니다.",
                )
            } else if (mode == StudyKind.ALPHABET) {
                val pagerState = rememberPagerState(pageCount = { 3 })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 1.dp),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        if (page == 0) {
                            AlphabetBoardFilterSelector(
                                selectedFilter = alphabetBoardFilter,
                                confusingCount = confusingAlphabetCards.size,
                                expanded = alphabetFilterExpanded,
                                onExpandedChange = { alphabetFilterExpanded = it },
                                onSelectFilter = {
                                    alphabetBoardFilter = it
                                    alphabetFilterExpanded = false
                                },
                            )
                            AlphabetSoundBoard(
                                modifier = Modifier.weight(1f),
                                alphabetCards = alphabetCards,
                                filter = alphabetBoardFilter,
                                weakLetterIds = state.weakLetterIds,
                            )
                        } else if (page == 1) {
                            AlphabetListeningQuiz(
                                modifier = Modifier.weight(1f),
                                title = "전체 알파벳 퀴즈",
                                alphabetCards = alphabetCards,
                                optionCount = alphabetQuizOptionCount,
                                onAnswer = viewModel::recordAlphabetQuizResult,
                            )
                        } else {
                            AlphabetListeningQuiz(
                                modifier = Modifier.weight(1f),
                                title = "헷갈림 복습 퀴즈",
                                alphabetCards = confusingAlphabetCards,
                                optionCount = alphabetQuizOptionCount,
                                onAnswer = viewModel::recordAlphabetQuizResult,
                            )
                        }
                    }
                }
            } else if (quizStyle) {
                WordMatchCard(
                    modifier = Modifier.weight(1f),
                    card = card,
                    reveal = state.revealed,
                    dense = dense,
                    onReveal = viewModel::revealAnswer,
                    onNext = { viewModel.submit(ReviewRating.GOOD) },
                )
            } else {
                StudyCardContent(
                    modifier = Modifier.weight(1f),
                    card = card,
                    reveal = state.revealed,
                    quizStyle = quizStyle,
                    dense = dense,
                    onToggleReveal = {
                        if (state.revealed) viewModel.hideAnswer() else viewModel.revealAnswer()
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::previousCard,
                    ) { Text("이전") }
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (state.revealed) viewModel.hideAnswer() else viewModel.revealAnswer()
                        },
                    ) { Text(if (state.revealed) "가리기" else "보기") }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::nextCard,
                    ) { Text("다음") }
                }

                AnimatedVisibility(
                    visible = state.revealed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    SoftCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            FilledTonalButton(
                                modifier = Modifier.weight(1f).heightIn(min = if (dense) 34.dp else 38.dp),
                                onClick = { viewModel.submit(ReviewRating.AGAIN) },
                            ) { Text("다시") }
                            FilledTonalButton(
                                modifier = Modifier.weight(1f).heightIn(min = if (dense) 34.dp else 38.dp),
                                onClick = { viewModel.submit(ReviewRating.HARD) },
                            ) { Text("어려움") }
                            Button(
                                modifier = Modifier.weight(1f).heightIn(min = if (dense) 34.dp else 38.dp),
                                onClick = { viewModel.submit(ReviewRating.GOOD) },
                            ) { Text("좋음") }
                            Button(
                                modifier = Modifier.weight(1f).heightIn(min = if (dense) 34.dp else 38.dp),
                                onClick = { viewModel.submit(ReviewRating.EASY) },
                            ) { Text("쉬움") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordMatchCard(
    modifier: Modifier = Modifier,
    card: StudyCard,
    reveal: Boolean,
    dense: Boolean,
    onReveal: () -> Unit,
    onNext: () -> Unit,
) {
    SoftCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !reveal, onClick = onReveal),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (dense) 6.dp else 8.dp, Alignment.CenterVertically),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (dense) 9.dp else 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (dense) 20.sp else 22.sp,
                            lineHeight = if (dense) 24.sp else 26.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    wordPronunciation(card)?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 17.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (reveal) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
                },
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = if (dense) 9.dp else 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (reveal) {
                            card.meanings.joinToString(", ").ifBlank { "뜻 없음" }
                        } else {
                            "뜻 보기"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp,
                        ),
                        color = if (reveal) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
            AnimatedVisibility(
                visible = reveal,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (dense) 6.dp else 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    card.exampleRu?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                    card.exampleKo?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (reveal) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (dense) 40.dp else 44.dp),
                onClick = onNext,
            ) { Text("다음") }
        } else {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (dense) 40.dp else 44.dp),
                onClick = onReveal,
            ) { Text("뜻 보기") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlphabetBoardFilterSelector(
    selectedFilter: AlphabetBoardFilter,
    confusingCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectFilter: (AlphabetBoardFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(
                modifier = Modifier.weight(1f),
                onClick = { onExpandedChange(!expanded) },
            ) {
                Text("필터: ${alphabetFilterLabel(selectedFilter)}")
            }
            Text(
                text = "옆으로 넘기면 퀴즈/헷갈림",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AlphabetFilterButton(
                    text = "전체",
                    selected = selectedFilter == AlphabetBoardFilter.ALL,
                    onClick = { onSelectFilter(AlphabetBoardFilter.ALL) },
                )
                AlphabetFilterButton(
                    text = "발음만",
                    selected = selectedFilter == AlphabetBoardFilter.PRONUNCIATION,
                    onClick = { onSelectFilter(AlphabetBoardFilter.PRONUNCIATION) },
                )
                AlphabetFilterButton(
                    text = if (confusingCount > 0) "헷갈림 $confusingCount" else "헷갈림",
                    selected = selectedFilter == AlphabetBoardFilter.WEAK,
                    enabled = confusingCount > 0,
                    onClick = { onSelectFilter(AlphabetBoardFilter.WEAK) },
                )
                AlphabetFilterButton(
                    text = "모음",
                    selected = selectedFilter == AlphabetBoardFilter.VOWEL,
                    onClick = { onSelectFilter(AlphabetBoardFilter.VOWEL) },
                )
                AlphabetFilterButton(
                    text = "자음",
                    selected = selectedFilter == AlphabetBoardFilter.CONSONANT,
                    onClick = { onSelectFilter(AlphabetBoardFilter.CONSONANT) },
                )
                AlphabetFilterButton(
                    text = "기호",
                    selected = selectedFilter == AlphabetBoardFilter.SIGN,
                    onClick = { onSelectFilter(AlphabetBoardFilter.SIGN) },
                )
            }
        }
    }
}

@Composable
private fun AlphabetFilterButton(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            contentPadding = ButtonDefaults.ContentPadding,
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            contentPadding = ButtonDefaults.ContentPadding,
        ) {
            Text(text)
        }
    }
}

private fun alphabetFilterLabel(filter: AlphabetBoardFilter): String {
    return when (filter) {
        AlphabetBoardFilter.ALL -> "전체"
        AlphabetBoardFilter.PRONUNCIATION -> "발음만"
        AlphabetBoardFilter.WEAK -> "헷갈림"
        AlphabetBoardFilter.VOWEL -> "모음"
        AlphabetBoardFilter.CONSONANT -> "자음"
        AlphabetBoardFilter.SIGN -> "기호"
    }
}

@Composable
private fun AlphabetSoundBoard(
    modifier: Modifier = Modifier,
    alphabetCards: List<StudyCard>,
    filter: AlphabetBoardFilter,
    weakLetterIds: Set<String>,
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var usageCard by remember { mutableStateOf<StudyCard?>(null) }
    val filteredCards = remember(alphabetCards, filter, weakLetterIds) {
        alphabetCards.filter { card ->
            when (filter) {
                AlphabetBoardFilter.ALL,
                AlphabetBoardFilter.PRONUNCIATION,
                -> true
                AlphabetBoardFilter.WEAK -> isConfusingAlphabetCard(card) || weakLetterIds.contains(card.remoteStableId)
                AlphabetBoardFilter.VOWEL -> card.structureLabel.equals("VOWEL", ignoreCase = true)
                AlphabetBoardFilter.CONSONANT -> card.structureLabel.equals("CONSONANT", ignoreCase = true)
                AlphabetBoardFilter.SIGN -> card.structureLabel.equals("SIGN", ignoreCase = true)
            }
        }
    }

    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeResult = engine.setLanguage(Locale("ru", "RU"))
                ttsReady = localeResult != TextToSpeech.LANG_MISSING_DATA &&
                    localeResult != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                ttsReady = false
            }
        }
        tts = engine
        onDispose {
            ttsReady = false
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(
            items = filteredCards,
            key = { it.remoteStableId },
        ) { card ->
            AlphabetSoundTile(
                letter = boardLetter(card.title),
                pronunciation = boardPronunciation(card),
                pronunciationOnly = filter == AlphabetBoardFilter.PRONUNCIATION,
                onLongClick = { usageCard = card },
                onClick = {
                    if (!ttsReady) return@AlphabetSoundTile
                    tts?.speak(
                        russianLetterForSpeech(card),
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "alphabet-${card.remoteStableId}-${System.currentTimeMillis()}",
                    )
                },
            )
        }
    }

    usageCard?.let { card ->
        AlphabetUsageDialog(
            card = card,
            onDismiss = { usageCard = null },
        )
    }
}

@Composable
private fun AlphabetListeningQuiz(
    modifier: Modifier = Modifier,
    title: String,
    alphabetCards: List<StudyCard>,
    optionCount: Int,
    onAnswer: (remoteStableId: String, correct: Boolean) -> Unit,
) {
    val pool = remember(alphabetCards) {
        alphabetCards.distinctBy { it.remoteStableId }
    }
    val questions = remember(pool) {
        pool
            .shuffled(Random(0x41A2BC3))
            .mapIndexed { index, card ->
                AlphabetQuizQuestion(
                    card = card,
                    direction = if (index % 2 == 0) AlphabetQuizDirection.KO_TO_RU else AlphabetQuizDirection.RU_TO_KO,
                )
            }
    }
    var questionIndex by rememberSaveable { mutableStateOf(0) }
    var correctCount by rememberSaveable { mutableStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var answered by rememberSaveable { mutableStateOf(false) }
    var answeredQuestion by remember { mutableStateOf<AlphabetQuizQuestion?>(null) }
    var answeredOptions by remember { mutableStateOf<List<StudyCard>?>(null) }

    val currentQuestion = questions.getOrNull(questionIndex)
    val currentOptions = currentQuestion?.let { question ->
        remember(question.card.remoteStableId, question.direction, pool) {
            buildAlphabetQuizOptions(
                target = question.card,
                pool = pool,
                direction = question.direction,
                optionCount = optionCount,
            )
        }
    }.orEmpty()
    val displayQuestion = answeredQuestion ?: currentQuestion
    val currentCard = displayQuestion?.card
    val options = answeredOptions ?: currentOptions

    SoftCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AlphabetBlueText,
            fontWeight = FontWeight.SemiBold,
        )
        if (questions.isEmpty()) {
            Text("퀴즈 카드가 없습니다.", style = MaterialTheme.typography.titleMedium)
            return@SoftCard
        }

        if (currentQuestion == null || currentCard == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AlphabetBlueLight,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "퀴즈 완료",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AlphabetBlueText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$correctCount / ${questions.size}",
                        style = MaterialTheme.typography.titleLarge,
                        color = AlphabetBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    questionIndex = 0
                    correctCount = 0
                    selectedId = null
                    answered = false
                    answeredQuestion = null
                    answeredOptions = null
                },
            ) {
                Text("다시 시작")
            }
            return@SoftCard
        }

        val direction = displayQuestion.direction
        val promptTitle = if (direction == AlphabetQuizDirection.KO_TO_RU) "한글 발음" else "러시아 글자"
        val promptValue = if (direction == AlphabetQuizDirection.KO_TO_RU) {
            alphabetPronunciationLabel(currentCard)
        } else {
            boardLetter(currentCard.title)
        }
        val solvedCount = (questionIndex + if (answered) 1 else 0).coerceAtMost(questions.size)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricChip(text = "문제 ${questionIndex + 1}/${questions.size}")
            MetricChip(text = "정답 $correctCount/$solvedCount")
        }
        LinearProgressIndicator(
            progress = { (questionIndex.toFloat() / questions.size.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = AlphabetBlue,
            trackColor = AlphabetBlueLight,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AlphabetBlueLight,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = promptTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = AlphabetBlue,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = promptValue,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                    ),
                    color = AlphabetBlueText,
                    textAlign = TextAlign.Center,
                )
            }
        }

        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { option ->
                    val isCorrect = option.remoteStableId == currentCard.remoteStableId
                    val isSelected = option.remoteStableId == selectedId
                    val prefix = when {
                        answered && isCorrect -> "✓ "
                        answered && isSelected && !isCorrect -> "✕ "
                        else -> ""
                    }
                    val containerColor = when {
                        answered && isCorrect -> AlphabetBlue
                        answered && isSelected && !isCorrect -> AlphabetBlueMid
                        isSelected -> AlphabetBlueMid
                        else -> AlphabetBlueLight
                    }
                    val contentColor = when {
                        answered && isCorrect -> Color.White
                        else -> AlphabetBlueText
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                        ),
                        onClick = {
                            if (answered) return@Button
                            val isAnswerCorrect = option.remoteStableId == currentCard.remoteStableId
                            selectedId = option.remoteStableId
                            answered = true
                            answeredQuestion = currentQuestion
                            answeredOptions = options
                            if (isAnswerCorrect) {
                                correctCount += 1
                            }
                            onAnswer(currentCard.remoteStableId, isAnswerCorrect)
                        },
                    ) {
                        val optionText = if (direction == AlphabetQuizDirection.KO_TO_RU) {
                            boardLetter(option.title)
                        } else {
                            alphabetPronunciationLabel(option)
                        }
                        Text(
                            text = prefix + optionText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (answered) {
            val isCorrect = selectedId == currentCard.remoteStableId
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isCorrect) AlphabetBlue.copy(alpha = 0.12f) else AlphabetBlueMid,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    text = if (isCorrect) {
                        "맞았어요."
                    } else {
                        if (direction == AlphabetQuizDirection.KO_TO_RU) {
                            "헷갈림에 추가됨 · 정답: ${boardLetter(currentCard.title)}"
                        } else {
                            "헷갈림에 추가됨 · 정답: ${alphabetPronunciationLabel(currentCard)}"
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = AlphabetBlueText,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    questionIndex += 1
                    selectedId = null
                    answered = false
                    answeredQuestion = null
                    answeredOptions = null
                },
            ) {
                Text(if (questionIndex >= questions.lastIndex) "결과 보기" else "다음 문제")
            }
        } else {
            Text(
                text = if (direction == AlphabetQuizDirection.KO_TO_RU) {
                    "한글을 보고 러시아 글자를 고르세요."
                } else {
                    "러시아 글자를 보고 한글 발음을 고르세요."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlphabetSoundTile(
    letter: String,
    pronunciation: String,
    pronunciationOnly: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = AlphabetBlueLight,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (pronunciationOnly) pronunciation else letter,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (pronunciationOnly) 14.sp else 22.sp,
                    lineHeight = if (pronunciationOnly) 17.sp else 24.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = AlphabetBlueText,
                textAlign = TextAlign.Center,
                maxLines = if (pronunciationOnly) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (pronunciationOnly) letter else pronunciation,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (pronunciationOnly) 17.sp else 11.sp,
                    lineHeight = if (pronunciationOnly) 19.sp else 13.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = AlphabetBlue,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlphabetUsageDialog(
    card: StudyCard,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = boardLetter(card.title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlphabetBlueText,
                )
                Text(
                    text = boardPronunciation(card),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    card.structureLabel?.let { AssistChip(onClick = {}, label = { Text(structureLabel(it)) }) }
                    card.usageFrequency?.let { AssistChip(onClick = {}, label = { Text("빈도: $it") }) }
                }
                AlphabetUsageText(label = "쓰임", value = card.soundFeel)
                AlphabetUsageText(label = "헷갈리는 짝", value = alphabetConfusionPair(card))
                AlphabetUsageText(label = "주의", value = card.confusionNote)
                AlphabetUsageText(label = "메모", value = card.tmiNote)
            }
        },
    )
}

@Composable
private fun AlphabetUsageText(
    label: String,
    value: String?,
) {
    val text = value?.trim().orEmpty()
    if (text.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = AlphabetBlue,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun isConfusingAlphabetCard(card: StudyCard): Boolean {
    return alphabetConfusionPair(card) != null
}

private fun alphabetConfusionPair(card: StudyCard): String? {
    return when (card.remoteStableId) {
        "alphabet:i",
        "alphabet:shorti",
        "alphabet:yeri",
        -> "И / Й / Ы: И는 그냥 '이', Й는 짧게 스치는 y, Ы는 혀를 뒤로 당기는 '으이' 느낌."
        "alphabet:ie",
        "alphabet:yo",
        "alphabet:e",
        -> "Е / Ё / Э: Е는 예/e, Ё는 요, Э는 입을 벌린 e. 점 두 개가 있으면 Ё."
        "alphabet:sha",
        "alphabet:shcha",
        -> "Ш / Щ: Ш는 짧고 두꺼운 sh, Щ는 더 길고 부드러운 shch."
        "alphabet:tse",
        "alphabet:che",
        -> "Ц / Ч: Ц는 ts, Ч는 ch. 한국어로는 '츠'와 '치' 쪽으로 나눠 듣기."
        "alphabet:ze",
        "alphabet:es",
        -> "З / С: З는 z, С는 s. 단어 끝에서는 З도 s처럼 약해질 수 있음."
        "alphabet:be",
        "alphabet:ve",
        -> "Б / В: Б는 b, В는 v. В는 영어 B처럼 보여도 소리는 v."
        "alphabet:er",
        "alphabet:pe",
        "alphabet:en",
        -> "Р / П / Н: Р은 r, П는 p, Н은 n. 영어 P/H 모양으로 읽으면 헷갈림."
        "alphabet:u",
        "alphabet:kha",
        -> "У / Х: У는 u, Х는 kh. 영어 Y/X처럼 보이는 모양 함정."
        "alphabet:hard",
        "alphabet:soft",
        -> "Ъ / Ь: 둘 다 소리보다 역할이 중요함. Ъ는 분리, Ь는 앞 자음을 부드럽게."
        else -> null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyCardContent(
    modifier: Modifier = Modifier,
    card: StudyCard,
    reveal: Boolean,
    quizStyle: Boolean,
    dense: Boolean,
    onToggleReveal: () -> Unit,
) {
    SoftCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleReveal),
    ) {
        if (quizStyle && !reveal) {
            Text(
                text = card.meanings.joinToString(", ").ifBlank { "뜻 없음" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = if (dense) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!dense) {
                Text(
                    text = "정답을 보기 전에 먼저 떠올려 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            wordPronunciation(card)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            card.structureLabel?.let { AssistChip(onClick = {}, label = { Text(structureLabel(it)) }) }
            card.usageFrequency?.let { AssistChip(onClick = {}, label = { Text("빈도: $it") }) }
            card.tone?.let { AssistChip(onClick = {}, label = { Text("톤: ${toneLabel(it)}") }) }
            card.contextTag?.let { AssistChip(onClick = {}, label = { Text("상황: $it") }) }
        }

        AnimatedVisibility(
            visible = reveal,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(if (dense) 4.dp else 6.dp)) {
                if (card.meanings.isNotEmpty()) {
                    Text(
                        text = "의미: ${card.meanings.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                card.situationHint?.let {
                    Text(text = "상황: $it", maxLines = 1, overflow = TextOverflow.Ellipsis)
                } ?: card.usageNote?.let {
                    Text(text = "메모: $it", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                card.exampleRu?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                card.exampleKo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun toneLabel(value: String): String {
    return when (value.uppercase()) {
        "FORMAL" -> "격식"
        "CASUAL" -> "친근"
        "PLAYFUL" -> "가벼움"
        "SLANG" -> "속어"
        else -> "중립"
    }
}

private fun structureLabel(value: String): String {
    return when (value.uppercase()) {
        "VOWEL" -> "모음"
        "SIGN" -> "기호"
        else -> "자음"
    }
}

private fun buildAlphabetQuizOptions(
    target: StudyCard,
    pool: List<StudyCard>,
    direction: AlphabetQuizDirection,
    optionCount: Int,
): List<StudyCard> {
    val safeOptionCount = optionCount.coerceAtLeast(1)
    val keySelector: (StudyCard) -> String = if (direction == AlphabetQuizDirection.KO_TO_RU) {
        { boardLetter(it.title) }
    } else {
        { koreanPronunciation(it) }
    }
    val distractors = pool
        .filter { it.remoteStableId != target.remoteStableId }
        .distinctBy(keySelector)
        .filter { keySelector(it) != keySelector(target) }
        .shuffled(Random(target.remoteStableId.hashCode() xor 0x3AA55CC))
        .take((safeOptionCount - 1).coerceAtLeast(0))

    return (distractors + target)
        .distinctBy(keySelector)
        .shuffled(Random(target.remoteStableId.hashCode() xor 0x5511CC7))
}

private fun boardLetter(title: String): String {
    val tokens = title.trim().split(" ").filter { it.isNotBlank() }
    return when {
        tokens.size >= 2 -> tokens[1]
        tokens.isNotEmpty() -> tokens.first()
        else -> title
    }
}

private fun boardPronunciation(card: StudyCard): String {
    return pronunciationLabel(card)
}

private fun wordPronunciation(card: StudyCard): String? {
    return pronunciationLabel(card)
        .takeIf { it.isNotBlank() }
        ?.let { "발음: $it" }
}

private fun koreanPronunciation(card: StudyCard): String {
    return alphabetKoreanPronunciation(card)
}

private fun pronunciationLabel(card: StudyCard): String {
    val hint = card.hint?.trim().orEmpty()
    val romanization = card.subtitle?.trim().orEmpty()
    return when {
        hint.isNotBlank() && romanization.isNotBlank() -> "$hint · /$romanization/"
        hint.isNotBlank() -> hint
        romanization.isNotBlank() -> "/$romanization/"
        else -> card.remoteStableId.substringAfter(":")
    }
}

private fun alphabetPronunciationLabel(card: StudyCard): String {
    return alphabetKoreanPronunciation(card)
}

private fun alphabetKoreanPronunciation(card: StudyCard): String {
    val hint = card.hint?.trim().orEmpty()
    return when {
        card.remoteStableId == "alphabet:shorti" -> "짧은 이"
        hint.isNotBlank() -> hint
        else -> card.remoteStableId.substringAfter(":")
    }
}

private fun russianLetterForSpeech(card: StudyCard): String {
    return boardLetter(card.title)
}

