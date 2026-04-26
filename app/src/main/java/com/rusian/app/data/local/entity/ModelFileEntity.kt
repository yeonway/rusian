package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "model_file",
    indices = [Index(value = ["filePath"], unique = true)],
)
data class ModelFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val fileSize: Long,
    val createdAt: Long,
)
