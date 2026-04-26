package com.rusian.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversation",
    foreignKeys = [
        ForeignKey(
            entity = ModelFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("updatedAt"), Index("modelId")],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val modelId: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
