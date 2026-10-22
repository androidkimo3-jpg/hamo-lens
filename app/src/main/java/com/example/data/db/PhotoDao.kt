package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY dateTaken DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM photos")
    suspend fun deleteAll()

    @Query("SELECT SUM(sizeBytes) FROM photos")
    fun getTotalSizeBytes(): Flow<Long?>
}
