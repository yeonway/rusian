package com.rusian.app.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rusian.app.ui.component.AppLoadingView
import com.rusian.app.ui.component.AppScreenContainer
import com.rusian.app.ui.component.MetricChip
import com.rusian.app.ui.component.ScreenContentPadding
import com.rusian.app.ui.component.SectionHeader
import com.rusian.app.ui.component.SoftCard
import com.rusian.app.ui.component.rememberIsCompactHeight
import com.rusian.app.ui.component.rememberIsDenseHeight
import com.rusian.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAlphabet: () -> Unit,
    onOpenFlashcard: () -> Unit,
    onOpenQuiz: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val compact = rememberIsCompactHeight()
    val dense = rememberIsDenseHeight()
    val spacing = if (dense) 6.dp else 8.dp

    if (state.loading) {
        AppScreenContainer {
            AppLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ScreenContentPadding),
                message = "오늘 학습 흐름을 준비하는 중입니다",
            )
        }
        return
    }

    val progressItems = state.categoryProgress.take(if (dense) 3 else 4)
    val remainingCount = (state.categoryProgress.size - progressItems.size).coerceAtLeast(0)

    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            SectionHeader(
                title = "오늘의 러시아어 학습",
                subtitle = if (dense) null else "짧고 자주, 부담 없이 이어가세요.",
            )

            SoftCard {
                Text(
                    text = "${state.dueTotal}",
                    style = MaterialTheme.typography.displaySmall,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MetricChip(text = "알파벳 ${state.dueAlphabet}")
                    MetricChip(text = "단어/표현 ${state.dueWords}")
                }
            }

            SoftCard {
                Text(
                    text = "바로 시작",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenAlphabet) { Text("알파벳") }
                        Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenFlashcard) { Text("카드") }
                        Button(modifier = Modifier.fillMaxWidth(), onClick = onOpenQuiz) { Text("퀴즈") }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Button(modifier = Modifier.weight(1f), onClick = onOpenAlphabet) { Text("알파벳") }
                        Button(modifier = Modifier.weight(1f), onClick = onOpenFlashcard) { Text("카드") }
                        Button(modifier = Modifier.weight(1f), onClick = onOpenQuiz) { Text("퀴즈") }
                    }
                }
            }

            SoftCard(modifier = Modifier.weight(1f)) {
                Text(text = "상황별 진행도", style = MaterialTheme.typography.titleMedium)
                progressItems.forEach { progress ->
                    val ratio = if (progress.total <= 0) 0f else progress.learned.toFloat() / progress.total.toFloat()
                    val animatedRatio by animateFloatAsState(
                        targetValue = ratio,
                        animationSpec = spring(stiffness = 250f),
                        label = "category-progress",
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${progress.categoryName}  ${progress.learned}/${progress.total}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { animatedRatio.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(if (dense) 4.dp else 5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
                if (remainingCount > 0) {
                    Text(
                        text = "+${remainingCount}개 카테고리",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::refresh,
            ) {
                Text("새로고침")
            }
        }
    }
}
