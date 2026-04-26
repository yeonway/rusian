package com.rusian.app.data.local

import androidx.room.TypeConverter
import com.rusian.app.domain.model.ReviewRating
import com.rusian.app.domain.model.StudyKind

class RoomConverters {
    @TypeConverter
    fun toStudyKind(value: String?): StudyKind? = value?.let { StudyKind.valueOf(it) }

    @TypeConverter
    fun fromStudyKind(value: StudyKind?): String? = value?.name

    @TypeConverter
    fun toReviewRating(value: String?): ReviewRating? = value?.let { ReviewRating.valueOf(it) }

    @TypeConverter
    fun fromReviewRating(value: ReviewRating?): String? = value?.name
}
