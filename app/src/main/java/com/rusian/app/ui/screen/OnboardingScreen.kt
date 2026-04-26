package com.rusian.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rusian.app.ui.component.AppLoadingView
import com.rusian.app.ui.component.AppScreenContainer
import com.rusian.app.ui.component.PrimaryActionButton
import com.rusian.app.ui.component.ScreenContentPadding
import com.rusian.app.ui.component.SectionHeader
import com.rusian.app.ui.component.SoftCard
import com.rusian.app.ui.component.rememberIsDenseHeight
import com.rusian.app.ui.viewmodel.OnboardingViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dense = rememberIsDenseHeight()
    val spacing = if (dense) 6.dp else 8.dp

    LaunchedEffect(state.completed) {
        if (state.completed) onComplete()
    }

    if (state.loading) {
        AppScreenContainer {
            AppLoadingView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ScreenContentPadding),
                message = "학습 설정을 준비하는 중입니다",
            )
        }
        return
    }

    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            SectionHeader(
                title = "학습 시작 설정",
                subtitle = if (dense) null else "필수만 선택하고 바로 시작하세요.",
            )

            SoftCard {
                Text(text = "하루 목표", style = MaterialTheme.typography.titleMedium)
                Text(text = "${state.dailyGoal}개 카드", style = MaterialTheme.typography.titleLarge)
                Slider(
                    value = state.dailyGoal.toFloat(),
                    onValueChange = { viewModel.updateGoal(it.toInt()) },
                    valueRange = 5f..100f,
                )
                if (!dense) {
                    Text(
                        text = "짧은 학습을 자주 반복하는 방식이 더 오래 기억됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SoftCard(modifier = Modifier.weight(1f)) {
                Text(text = "카테고리 선택", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.categories.forEach { category ->
                        val selected = state.selectedCategoryIds.contains(category.id)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.toggleCategory(category.id) },
                            label = {
                                Text(
                                    text = category.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }

            PrimaryActionButton(
                text = "학습 시작하기",
                onClick = viewModel::complete,
            )
        }
    }
}
