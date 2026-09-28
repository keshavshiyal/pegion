package com.pegion.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pegion.data.local.entity.DownloadEntity
import com.pegion.data.repository.DownloadRepository
import com.pegion.download.engine.ChecksumVerifier
import com.pegion.download.model.DownloadSegment
import com.pegion.download.model.DownloadStatus
import com.pegion.download.model.LiveDownloadStats
import com.pegion.download.model.SpeedSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class DetailsViewModel(
    private val downloadId: Long,
    private val repository: DownloadRepository
) : ViewModel() {

    val download: StateFlow<DownloadEntity?> = combine(
        repository.getDownloadById(downloadId),
        repository.liveStatsFlow
    ) { entity, liveStatsMap ->
        if (entity == null) return@combine null
        val live = liveStatsMap[downloadId]
        if (live != null && (entity.status == DownloadStatus.DOWNLOADING || entity.status == DownloadStatus.PENDING)) {
            entity.copy(
                downloadedBytes = live.downloadedBytes,
                fileSize = if (live.fileSize > 0) live.fileSize else entity.fileSize,
                progress = live.progress,
                speed = live.speed,
                eta = live.eta
            )
        } else {
            entity
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val segments: StateFlow<List<DownloadSegment>> = combine(
        repository.getSegments(downloadId),
        repository.liveStatsFlow
    ) { dbSegments, liveStatsMap ->
        val live = liveStatsMap[downloadId]
        if (live != null && live.segments.isNotEmpty()) {
            live.segments
        } else {
            dbSegments.map {
                DownloadSegment(
                    segmentIndex = it.segmentIndex,
                    startByte = it.startByte,
                    endByte = it.endByte,
                    downloadedBytes = it.downloadedBytes,
                    isFinished = it.isFinished
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _speedHistory = MutableStateFlow<List<SpeedSample>>(emptyList())
    val speedHistory: StateFlow<List<SpeedSample>> = _speedHistory.asStateFlow()

    private val _manualChecksumResult = MutableStateFlow<String?>(null)
    val manualChecksumResult: StateFlow<String?> = _manualChecksumResult.asStateFlow()

    init {
        // Poll speed history updates periodically
        viewModelScope.launch {
            while (true) {
                _speedHistory.value = repository.getSpeedHistory(downloadId)
                delay(1000)
            }
        }

        // Auto-heal file size from disk for completed downloads if missing
        viewModelScope.launch {
            download.collect { entity ->
                if (entity != null && entity.status == DownloadStatus.COMPLETED) {
                    if (entity.fileSize <= 0 || entity.downloadedBytes <= 0) {
                        val file = File(entity.filePath)
                        if (file.exists() && file.length() > 0) {
                            repository.updateFileSize(entity.id, file.length())
                        }
                    }
                }
            }
        }
    }

    fun pause() = repository.pauseDownload(downloadId)

    fun resume() = repository.resumeDownload(downloadId)

    fun cancel() = repository.cancelDownload(downloadId)

    fun retry() = repository.retryDownload(downloadId)

    fun delete(deleteFileFromStorage: Boolean) =
        repository.deleteDownload(downloadId, deleteFileFromStorage)

    fun computeChecksum(algorithm: String) {
        viewModelScope.launch {
            val entity = download.value ?: return@launch
            val file = File(entity.filePath)
            if (file.exists()) {
                val hash = ChecksumVerifier.calculateChecksum(file, algorithm)
                _manualChecksumResult.value = "$algorithm: $hash"
            } else {
                _manualChecksumResult.value = "File not found on disk"
            }
        }
    }

    companion object {
        fun provideFactory(
            downloadId: Long,
            repository: DownloadRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailsViewModel(downloadId, repository) as T
            }
        }
    }
}
