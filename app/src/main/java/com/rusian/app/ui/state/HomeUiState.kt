package com.rusian.app.ui.state

import com.rusian.app.domain.model.CategoryProgress

data class HomeUiState(
    val loading: Boolean = true,
    val dueTotal: Int = 0,
    val dueAlphabet: Int = 0,
    val dueWords: Int = 0,
    val categoryProgress: List<CategoryProgress> = emptyList(),
)
