package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rusian.app.domain.model.StudyKind

@Entity(
    tableName = "study_item",
    indices = [Index(value = ["remoteStableId"], unique = true), Index("kind"), Index("isVisible")],
)
data class StudyItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: StudyKind,
    val remoteStableId: String,
    val alphabetId: String?,
    val wordId: String?,
    val isVisible: Boolean,
    val createdAt: Long,
)
