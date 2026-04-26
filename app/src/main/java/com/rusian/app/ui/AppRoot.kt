package com.rusian.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rusian.app.domain.model.StudyKind
import com.rusian.app.ui.component.AppLoadingView
import com.rusian.app.ui.navigation.AppDestination
import com.rusian.app.ui.navigation.MainBottomItems
import com.rusian.app.ui.screen.ChatScreen
import com.rusian.app.ui.screen.OnboardingScreen
import com.rusian.app.ui.screen.SettingsScreen
import com.rusian.app.ui.screen.StudyScreen
import com.rusian.app.ui.viewmodel.ChatViewModel
import com.rusian.app.ui.viewmodel.MainViewModel
import com.rusian.app.ui.viewmodel.OnboardingViewModel
import com.rusian.app.ui.viewmodel.StudyViewModel
import com.rusian.app.ui.viewmodel.SyncViewModel

@Composable
fun AppRoot(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val onboardingDone by mainViewModel.onboardingDone.collectAsStateWithLifecycle()
    val showExpressionCards by mainViewModel.showExpressionCards.collectAsStateWithLifecycle()
    val alphabetQuizOptionCount by mainViewModel.alphabetQuizOptionCount.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val startDestination = when (onboardingDone) {
        null -> null
        true -> AppDestination.QUIZ
        false -> AppDestination.ONBOARDING
    }

    if (startDestination == null) {
        AppLoadingView(message = "앱을 준비하는 중입니다")
        return
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute != AppDestination.ONBOARDING

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                ) {
                    MainBottomItems
                        .filter { item -> showExpressionCards || item.route != AppDestination.FLASHCARD }
                        .forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(AppDestination.QUIZ) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.ONBOARDING) {
                val vm: OnboardingViewModel = hiltViewModel()
                OnboardingScreen(
                    viewModel = vm,
                    onComplete = {
                        navController.navigate(AppDestination.QUIZ) {
                            popUpTo(AppDestination.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(AppDestination.ALPHABET) {
                val vm: StudyViewModel = hiltViewModel()
                StudyScreen(
                    title = "알파벳 감각 익히기",
                    mode = StudyKind.ALPHABET,
                    viewModel = vm,
                    quizStyle = false,
                    alphabetQuizOptionCount = alphabetQuizOptionCount,
                )
            }
            composable(AppDestination.FLASHCARD) {
                val vm: StudyViewModel = hiltViewModel()
                StudyScreen(
                    title = "표현 카드",
                    mode = StudyKind.WORD,
                    viewModel = vm,
                    quizStyle = false,
                    alphabetQuizOptionCount = alphabetQuizOptionCount,
                )
            }
            composable(AppDestination.QUIZ) {
                val vm: StudyViewModel = hiltViewModel()
                StudyScreen(
                    title = "단어 뜻 보기",
                    mode = StudyKind.WORD,
                    viewModel = vm,
                    quizStyle = true,
                    alphabetQuizOptionCount = alphabetQuizOptionCount,
                )
            }
            composable(AppDestination.CHAT) {
                val vm: ChatViewModel = hiltViewModel()
                ChatScreen(viewModel = vm)
            }
            composable(AppDestination.SETTINGS) {
                val vm: SyncViewModel = hiltViewModel()
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
