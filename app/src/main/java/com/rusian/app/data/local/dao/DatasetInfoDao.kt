package com.rusian.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rusian.app.data.local.entity.DatasetInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DatasetInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DatasetInfoEntity)

    @Query("SELECT * FROM dataset_info WHERE id = 1 LIMIT 1")
    suspend fun get(): DatasetInfoEntity?

    @Query("SELECT * FROM dataset_info WHERE id = 1 LIMIT 1")
    fun observe(): Flow<DatasetInfoEntity?>

    @Query("DELETE FROM dataset_info")
    suspend fun clear()
}
