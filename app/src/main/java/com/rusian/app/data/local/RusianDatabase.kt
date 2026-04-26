package com.rusian.app.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rusian.app.data.local.dao.AlphabetLetterDao
import com.rusian.app.data.local.dao.ChatMessageDao
import com.rusian.app.data.local.dao.CategoryDao
import com.rusian.app.data.local.dao.ConversationDao
import com.rusian.app.data.local.dao.DatasetInfoDao
import com.rusian.app.data.local.dao.ModelFileDao
import com.rusian.app.data.local.dao.ReviewEventDao
import com.rusian.app.data.local.dao.ReviewStateDao
import com.rusian.app.data.local.dao.StudyItemDao
import com.rusian.app.data.local.dao.WordDao
import com.rusian.app.data.local.dao.WordGlossDao
import com.rusian.app.data.local.entity.AlphabetLetterEntity
import com.rusian.app.data.local.entity.ChatMessageEntity
import com.rusian.app.data.local.entity.CategoryEntity
import com.rusian.app.data.local.entity.ConversationEntity
import com.rusian.app.data.local.entity.DatasetInfoEntity
import com.rusian.app.data.local.entity.ModelFileEntity
import com.rusian.app.data.local.entity.ReviewEventEntity
import com.rusian.app.data.local.entity.ReviewStateEntity
import com.rusian.app.data.local.entity.StudyItemEntity
import com.rusian.app.data.local.entity.WordEntity
import com.rusian.app.data.local.entity.WordGlossEntity

@Database(
    entities = [
        CategoryEntity::class,
        AlphabetLetterEntity::class,
        WordEntity::class,
        WordGlossEntity::class,
        StudyItemEntity::class,
        ReviewStateEntity::class,
        ReviewEventEntity::class,
        DatasetInfoEntity::class,
        ModelFileEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class RusianDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun alphabetLetterDao(): AlphabetLetterDao
    abstract fun wordDao(): WordDao
    abstract fun wordGlossDao(): WordGlossDao
    abstract fun studyItemDao(): StudyItemDao
    abstract fun reviewStateDao(): ReviewStateDao
    abstract fun reviewEventDao(): ReviewEventDao
    abstract fun datasetInfoDao(): DatasetInfoDao
    abstract fun modelFileDao(): ModelFileDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alphabet_letter ADD COLUMN letterType TEXT NOT NULL DEFAULT 'CONSONANT'")
                db.execSQL("ALTER TABLE alphabet_letter ADD COLUMN soundFeel TEXT")
                db.execSQL("ALTER TABLE alphabet_letter ADD COLUMN confusionNote TEXT")
                db.execSQL("ALTER TABLE alphabet_letter ADD COLUMN usageFrequency TEXT")
                db.execSQL("ALTER TABLE alphabet_letter ADD COLUMN tmiNote TEXT")

                db.execSQL("ALTER TABLE word ADD COLUMN tone TEXT NOT NULL DEFAULT 'NEUTRAL'")
                db.execSQL("ALTER TABLE word ADD COLUMN situationHint TEXT")
                db.execSQL("ALTER TABLE word ADD COLUMN usageNote TEXT")
                db.execSQL("ALTER TABLE word ADD COLUMN pairKey TEXT")
                db.execSQL("ALTER TABLE word ADD COLUMN contextTag TEXT")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_file` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_model_file_filePath` ON `model_file` (`filePath`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversation` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `modelId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`modelId`) REFERENCES `model_file`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_conversation_updatedAt` ON `conversation` (`updatedAt`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_conversation_modelId` ON `conversation` (`modelId`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_message` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conversationId` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`conversationId`) REFERENCES `conversation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_chat_message_conversationId_createdAt`
                    ON `chat_message` (`conversationId`, `createdAt`)
                    """.trimIndent(),
                )
            }
        }
    }
}
