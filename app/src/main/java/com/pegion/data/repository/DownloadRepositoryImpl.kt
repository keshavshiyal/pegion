package com.pegion.data.repository

import android.content.Context
import com.pegion.data.local.dao.DownloadDao
import com.pegion.data.local.dao.DownloadSegmentDao
import com.pegion.data.local.entity.DownloadEntity
import com.pegion.data.local.entity.DownloadSegmentEntity
import com.pegion.data.datastore.UserPreferencesRepository
import com.pegion.download.engine.DownloadEngine
import com.pegion.download.model.DownloadPriority
import com.pegion.download.model.DownloadStatus
import com.pegion.download.model.LiveDownloadStats
import com.pegion.download.model.SpeedSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File

class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val downloadSegmentDao: DownloadSegmentDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val downloadEngine: DownloadEngine
) : DownloadRepository {

    override val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    override val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    override val totalSpeedFlow: StateFlow<Long> = downloadEngine.totalSpeedFlow
    override val liveStatsFlow: StateFlow<Map<Long, LiveDownloadStats>> = downloadEngine.liveStatsFlow

    override fun getDownloadById(id: Long): Flow<DownloadEntity?> = downloadDao.getDownloadById(id)

    override suspend fun getDownloadByIdSync(id: Long): DownloadEntity? = downloadDao.getDownloadByIdSync(id)

    override fun getSegments(downloadId: Long): Flow<List<DownloadSegmentEntity>> =
        downloadSegmentDao.getSegmentsFlow(downloadId)

    override suspend fun enqueueDownload(
        url: String,
        suggestedFileName: String?,
        priority: DownloadPriority,
        checksumType: String?,
        checksumValue: String?,
        speedLimit: Long
    ): Long {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        val finalFileName = DownloadEngine.resolveFileName(url, suggestedFileName)
        val downloadFolder = DownloadEngine.getDownloadDirectory(context, prefs.downloadFolder)
        val targetFile = File(downloadFolder, finalFileName)

        val entity = DownloadEntity(
            url = url.trim(),
            fileName = finalFileName,
            filePath = targetFile.absolutePath,
            status = DownloadStatus.PENDING,
            priority = priority,
            checksumType = checksumType,
            checksumValue = checksumValue?.trim(),
            speedLimit = speedLimit
        )

        val id = downloadDao.insertDownload(entity)
        downloadEngine.triggerQueueProcessing()
        return id
    }

    override suspend fun enqueueBatch(urls: List<String>, priority: DownloadPriority): List<Long> {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        val downloadFolder = DownloadEngine.getDownloadDirectory(context, prefs.downloadFolder)
        val entities = urls.filter { it.isNotBlank() }.map { rawUrl ->
            val url = rawUrl.trim()
            val finalFileName = DownloadEngine.resolveFileName(url, null)
            val targetFile = File(downloadFolder, finalFileName)
            DownloadEntity(
                url = url,
                fileName = finalFileName,
                filePath = targetFile.absolutePath,
                status = DownloadStatus.PENDING,
                priority = priority
            )
        }
        val ids = downloadDao.insertAll(entities)
        downloadEngine.triggerQueueProcessing()
        return ids
    }

    override suspend fun isDuplicateUrl(url: String): Boolean {
        return downloadDao.countByUrl(url.trim()) > 0
    }

    override fun pauseDownload(id: Long) = downloadEngine.pauseDownload(id)

    override fun resumeDownload(id: Long) = downloadEngine.resumeDownload(id)

    override fun cancelDownload(id: Long) = downloadEngine.cancelDownload(id)

    override fun retryDownload(id: Long) = downloadEngine.retryDownload(id)

    override fun deleteDownload(id: Long, deleteFileFromStorage: Boolean) =
        downloadEngine.deleteDownload(id, deleteFileFromStorage)

    override suspend fun renameDownload(id: Long, newName: String) =
        downloadDao.renameDownload(id, newName.trim())

    override suspend fun clearCompleted() =
        downloadDao.deleteByStatus(DownloadStatus.COMPLETED)

    override suspend fun clearAll() =
        downloadDao.clearAll()

    override suspend fun updateFileSize(id: Long, size: Long) =
        downloadDao.updateFileSize(id, size)

    override fun getSpeedHistory(id: Long): List<SpeedSample> =
        downloadEngine.getSpeedHistory(id)

    override suspend fun probeUrl(url: String): com.pegion.download.model.UrlProbeResult =
        downloadEngine.probeUrl(url)
}
