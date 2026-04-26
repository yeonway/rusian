package com.rusian.app.data.local.model

data class CategoryProgressRow(
    val categoryId: String,
    val categoryName: String,
    val total: Int,
    val learned: Int,
)
