package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getDownloadById(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadByIdSync(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    suspend fun getDownloadsByStatusSync(status: DownloadStatus): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING') ORDER BY priority DESC, createdAt ASC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING') ORDER BY priority DESC, createdAt ASC")
    suspend fun getActiveDownloadsSync(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADING'")
    suspend fun getCurrentlyDownloadingSync(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): DownloadEntity?

    @Query("SELECT COUNT(*) FROM downloads WHERE url = :url")
    suspend fun countByUrl(url: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(downloads: List<DownloadEntity>): List<Long>

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String? = null)

    @Query("""
        UPDATE downloads 
        SET downloadedBytes = :downloadedBytes, 
            fileSize = CASE WHEN :fileSize > 0 THEN :fileSize ELSE fileSize END,
            progress = :progress, 
            speed = :speed, 
            eta = :eta,
            status = 'DOWNLOADING'
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: Long,
        downloadedBytes: Long,
        fileSize: Long,
        progress: Float,
        speed: Long,
        eta: Long
    )

    @Query("UPDATE downloads SET status = 'COMPLETED', progress = 100.0, speed = 0, eta = 0, completedAt = :completedAt, actualChecksum = :actualChecksum WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Long, actualChecksum: String?)

    @Query("UPDATE downloads SET fileName = :newName WHERE id = :id")
    suspend fun renameDownload(id: Long, newName: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteByStatus(status: DownloadStatus)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
