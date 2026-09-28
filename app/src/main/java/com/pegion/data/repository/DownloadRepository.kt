package com.pegion.data.repository

import com.pegion.data.local.entity.DownloadEntity
import com.pegion.data.local.entity.DownloadSegmentEntity
import com.pegion.download.model.DownloadPriority
import com.pegion.download.model.DownloadStatus
import com.pegion.download.model.LiveDownloadStats
import com.pegion.download.model.SpeedSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val allDownloads: Flow<List<DownloadEntity>>
    val activeDownloads: Flow<List<DownloadEntity>>
    val totalSpeedFlow: StateFlow<Long>
    val liveStatsFlow: StateFlow<Map<Long, LiveDownloadStats>>

    fun getDownloadById(id: Long): Flow<DownloadEntity?>
    suspend fun getDownloadByIdSync(id: Long): DownloadEntity?
    fun getSegments(downloadId: Long): Flow<List<DownloadSegmentEntity>>
    suspend fun enqueueDownload(
        url: String,
        suggestedFileName: String? = null,
        priority: DownloadPriority = DownloadPriority.NORMAL,
        checksumType: String? = null,
        checksumValue: String? = null,
        speedLimit: Long = 0L
    ): Long

    suspend fun enqueueBatch(urls: List<String>, priority: DownloadPriority = DownloadPriority.NORMAL): List<Long>
    suspend fun isDuplicateUrl(url: String): Boolean
    fun pauseDownload(id: Long)
    fun resumeDownload(id: Long)
    fun cancelDownload(id: Long)
    fun retryDownload(id: Long)
    fun deleteDownload(id: Long, deleteFileFromStorage: Boolean)
    suspend fun renameDownload(id: Long, newName: String)
    suspend fun clearCompleted()
    suspend fun clearAll()
    suspend fun updateFileSize(id: Long, size: Long)
    fun getSpeedHistory(id: Long): List<SpeedSample>
}
