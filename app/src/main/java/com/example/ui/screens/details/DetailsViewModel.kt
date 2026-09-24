package com.example.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.example.download.engine.ChecksumVerifier
import com.example.download.model.SpeedSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class DetailsViewModel(
    private val downloadId: Long,
    private val repository: DownloadRepository
) : ViewModel() {

    val download: StateFlow<DownloadEntity?> = repository.getDownloadById(downloadId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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
