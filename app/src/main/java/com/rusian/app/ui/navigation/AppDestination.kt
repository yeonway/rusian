package com.rusian.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.ui.graphics.vector.ImageVector

object AppDestination {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ALPHABET = "alphabet"
    const val FLASHCARD = "flashcard"
    const val QUIZ = "quiz"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val MainBottomItems = listOf(
    BottomItem(AppDestination.ALPHABET, "알파벳", Icons.Outlined.Spellcheck),
    BottomItem(AppDestination.FLASHCARD, "카드", Icons.AutoMirrored.Outlined.MenuBook),
    BottomItem(AppDestination.QUIZ, "퀴즈", Icons.Outlined.Psychology),
    BottomItem(AppDestination.CHAT, "채팅", Icons.Outlined.Forum),
    BottomItem(AppDestination.SETTINGS, "설정", Icons.Outlined.Settings),
)
