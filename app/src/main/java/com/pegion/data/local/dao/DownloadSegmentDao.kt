package com.pegion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pegion.data.local.entity.DownloadSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadSegmentDao {

    @Query("SELECT * FROM download_segments WHERE downloadId = :downloadId ORDER BY segmentIndex ASC")
    fun getSegmentsFlow(downloadId: Long): Flow<List<DownloadSegmentEntity>>

    @Query("SELECT * FROM download_segments WHERE downloadId = :downloadId ORDER BY segmentIndex ASC")
    suspend fun getSegmentsSync(downloadId: Long): List<DownloadSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<DownloadSegmentEntity>)

    @Update
    suspend fun updateSegment(segment: DownloadSegmentEntity)

    @Query("UPDATE download_segments SET downloadedBytes = :downloadedBytes, isFinished = :isFinished WHERE downloadId = :downloadId AND segmentIndex = :segmentIndex")
    suspend fun updateSegmentProgress(downloadId: Long, segmentIndex: Int, downloadedBytes: Long, isFinished: Boolean)

    @Query("DELETE FROM download_segments WHERE downloadId = :downloadId")
    suspend fun deleteSegmentsForDownload(downloadId: Long)
}
