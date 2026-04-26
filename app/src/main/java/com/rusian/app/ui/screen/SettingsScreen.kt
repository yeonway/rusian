package com.rusian.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rusian.app.data.preferences.SyncPolicy
import com.rusian.app.domain.model.ThemeMode
import com.rusian.app.ui.component.AppScreenContainer
import com.rusian.app.ui.component.MetricChip
import com.rusian.app.ui.component.ScreenContentPadding
import com.rusian.app.ui.component.SectionHeader
import com.rusian.app.ui.component.SoftCard
import com.rusian.app.ui.component.rememberIsDenseHeight
import com.rusian.app.ui.viewmodel.SyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SyncViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dense = rememberIsDenseHeight()
    val spacing = if (dense) 6.dp else 8.dp
    val scrollState = rememberScrollState()
    var showCleanInstallConfirm by remember { mutableStateOf(false) }
    var showFactoryResetStep1 by remember { mutableStateOf(false) }
    var showFactoryResetStep2 by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA) }
    val lastSyncText = state.status.lastSyncAt?.let { dateFormat.format(Date(it)) } ?: "기록 없음"

    AppScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            SectionHeader(
                title = "설정",
                subtitle = if (dense) null else "학습, 채팅, 백업, 업데이트 동작을 조정합니다.",
            )

            AnimatedVisibility(
                visible = !state.message.isNullOrBlank() || !state.status.lastMessage.isNullOrBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SoftCard {
                    state.message?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                    state.status.lastMessage?.let {
                        Text(
                            text = "로그: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SoftCard {
                Text(text = "화면", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(themeModeLabel(mode), maxLines = 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            SoftCard {
                Text(text = "학습", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "하루 목표 ${state.dailyGoal}개",
                    style = MaterialTheme.typography.titleLarge,
                )
                Slider(
                    value = state.dailyGoal.toFloat(),
                    onValueChange = { viewModel.setDailyGoal(it.roundToInt()) },
                    valueRange = 5f..100f,
                    steps = 18,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(10, 20, 30, 50, 100).forEach { goal ->
                        FilterChip(
                            selected = state.dailyGoal == goal,
                            onClick = { viewModel.setDailyGoal(goal) },
                            label = { Text("${goal}개") },
                        )
                    }
                }
            }

            SoftCard {
                Text(text = "학습 화면", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "표현 카드는 초보자에게는 숨겨도 됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = !state.showExpressionCards,
                        onClick = { viewModel.setShowExpressionCards(false) },
                        label = { Text("표현 카드 숨김") },
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = state.showExpressionCards,
                        onClick = { viewModel.setShowExpressionCards(true) },
                        label = { Text("표현 카드 표시") },
                    )
                }
            }

            SoftCard {
                Text(text = "알파벳 퀴즈", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "선택지 ${state.alphabetQuizOptionCount}개",
                    style = MaterialTheme.typography.titleLarge,
                )
                Slider(
                    value = state.alphabetQuizOptionCount.toFloat(),
                    onValueChange = { viewModel.setAlphabetQuizOptionCount(it.roundToInt()) },
                    valueRange = 4f..12f,
                    steps = 7,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(4, 6, 8, 10, 12).forEach { count ->
                        FilterChip(
                            selected = state.alphabetQuizOptionCount == count,
                            onClick = { viewModel.setAlphabetQuizOptionCount(count) },
                            label = { Text("${count}개") },
                        )
                    }
                }
            }

            SoftCard {
                Text(text = "학습 카테고리", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (state.selectedCategoryIds.isEmpty()) {
                        "전체 카테고리 학습"
                    } else {
                        "${state.selectedCategoryIds.size}개 카테고리만 학습"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = state.selectedCategoryIds.isEmpty(),
                        onClick = viewModel::clearCategoryFilter,
                        label = { Text("전체") },
                    )
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.selectedCategoryIds.contains(category.id),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::selectAllCategories,
                    ) { Text("모두 선택") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::clearCategoryFilter,
                    ) { Text("전체 학습") }
                }
            }

            SoftCard {
                Text(text = "동기화", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "현재 데이터셋: ${state.status.currentDatasetVersion ?: "설치되지 않음"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "마지막 동기화: $lastSyncText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.updateAvailable) {
                    MetricChip(text = "업데이트 가능: ${state.remoteVersion ?: "버전 없음"}")
                }
                if (state.remoteWordCount > 0 || state.localWordCount > 0) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MetricChip(text = "새 단어 ${state.addedWordCount}개")
                        MetricChip(text = "원격 ${state.remoteWordCount}개")
                        MetricChip(text = "현재 ${state.localWordCount}개")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SyncPolicy.entries.forEach { policy ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = state.syncPolicy == policy,
                            onClick = { viewModel.setSyncPolicy(policy) },
                            label = { Text(syncPolicyLabel(policy), maxLines = 1) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.manifestUrlInput,
                    onValueChange = viewModel::onManifestUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("content-pack.json URL") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::saveManifestUrl,
                    ) { Text("주소 저장") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::resetManifestUrl,
                    ) { Text("기본값") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::checkUpdates,
                        enabled = state.remoteUpdatesConfigured && !state.checking && !state.updating,
                    ) {
                        Text(if (state.checking) "확인 중" else "업데이트 확인")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::runIncrementalUpdate,
                        enabled = state.remoteUpdatesConfigured && !state.updating,
                    ) {
                        Text(if (state.updating) "진행 중" else "증분 업데이트")
                    }
                }
                if (!state.remoteUpdatesConfigured) {
                    Text(
                        text = "GitHub raw content-pack.json 주소를 저장하면 원격 업데이트를 사용할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SoftCard {
                Text(text = "앱 업데이트", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "현재 버전: ${state.currentAppVersion.ifBlank { "확인 전" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.latestAppVersion?.let { version ->
                    MetricChip(
                        text = if (state.appUpdateAvailable) {
                            "새 버전 $version"
                        } else {
                            "최신 버전 $version"
                        },
                    )
                }
                state.appUpdateApkName?.let { apkName ->
                    Text(
                        text = apkName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::checkAppUpdate,
                        enabled = !state.checkingAppUpdate && !state.downloadingAppUpdate,
                    ) {
                        Text(if (state.checkingAppUpdate) "확인 중" else "새 버전 확인")
                    }
                    if (state.appUpdateDownloaded) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::installDownloadedAppUpdate,
                            enabled = !state.checkingAppUpdate && !state.downloadingAppUpdate,
                        ) {
                            Text("설치하기")
                        }
                    } else {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::downloadAppUpdate,
                            enabled = state.appUpdateDownloadUrl != null &&
                                !state.checkingAppUpdate &&
                                !state.downloadingAppUpdate,
                        ) {
                            Text(if (state.downloadingAppUpdate) "다운로드 중" else "다운로드")
                        }
                    }
                }
                Text(
                    text = "GitHub Releases에 APK를 올리면 여기서 받아 설치할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SoftCard {
                Text(text = "백업", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::createBackup,
                    ) { Text("백업 생성") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showRestoreConfirm = true },
                    ) { Text("최근 백업 복원") }
                }
            }

            SoftCard {
                Text(text = "채팅", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::clearLastChatRoom,
                ) { Text("마지막 채팅방 기억 지우기") }
            }

            SoftCard {
                Text(text = "데이터 관리", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showCleanInstallConfirm = true },
                        enabled = !state.updating,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("콘텐츠 새로 설치")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showFactoryResetStep1 = true },
                        enabled = !state.updating,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("학습 기록 초기화")
                    }
                }
            }
        }
    }

    if (showCleanInstallConfirm) {
        AlertDialog(
            onDismissRequest = { showCleanInstallConfirm = false },
            title = { Text("콘텐츠 새로 설치") },
            text = { Text("콘텐츠를 다시 설치합니다. 가능한 학습 진행 기록은 최대한 보존합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanInstallConfirm = false
                        viewModel.runCleanInstall()
                    },
                ) { Text("실행") }
            },
            dismissButton = {
                TextButton(onClick = { showCleanInstallConfirm = false }) { Text("취소") }
            },
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("최근 백업 복원") },
            text = { Text("최근 백업의 학습 기록으로 되돌립니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        viewModel.restoreLatestBackup()
                    },
                ) { Text("복원") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("취소") }
            },
        )
    }

    if (showFactoryResetStep1) {
        AlertDialog(
            onDismissRequest = { showFactoryResetStep1 = false },
            title = { Text("학습 기록 초기화 1/2") },
            text = { Text("학습 진행률과 리뷰 기록이 초기화됩니다. 마지막 확인 단계로 이동할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFactoryResetStep1 = false
                        showFactoryResetStep2 = true
                    },
                ) { Text("다음") }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetStep1 = false }) { Text("취소") }
            },
        )
    }

    if (showFactoryResetStep2) {
        AlertDialog(
            onDismissRequest = { showFactoryResetStep2 = false },
            title = { Text("학습 기록 초기화 2/2") },
            text = { Text("마지막 확인입니다. 초기화 전에 자동 백업을 생성합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFactoryResetStep2 = false
                        viewModel.runFactoryReset()
                    },
                ) { Text("지금 초기화") }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetStep2 = false }) { Text("취소") }
            },
        )
    }
}

private fun themeModeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "시스템"
        ThemeMode.LIGHT -> "라이트"
        ThemeMode.DARK -> "다크"
    }
}

private fun syncPolicyLabel(policy: SyncPolicy): String {
    return when (policy) {
        SyncPolicy.WIFI_OR_CHARGING -> "Wi-Fi/충전"
        SyncPolicy.ANY_NETWORK -> "항상"
    }
}
