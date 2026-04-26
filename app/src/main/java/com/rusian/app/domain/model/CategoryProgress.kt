package com.rusian.app.domain.model

data class CategoryProgress(
    val categoryId: String,
    val categoryName: String,
    val total: Int,
    val learned: Int,
)
