package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dataset_info")
data class DatasetInfoEntity(
    @PrimaryKey val id: Int = 1,
    val schemaVersion: Int,
    val datasetVersion: String,
    val sha256: String?,
    val installedAt: Long,
    val source: String,
)
