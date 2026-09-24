package com.example.ui.screens.home

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.local.entity.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.example.download.model.DownloadFilter
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadSortOrder
import com.example.download.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeSummaryMetrics(
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val pausedCount: Int = 0,
    val failedCount: Int = 0,
    val totalBytesDownloaded: Long = 0L,
    val globalSpeed: Long = 0L
)

class HomeViewModel(
    private val repository: DownloadRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val context: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    val selectedFilter: StateFlow<DownloadFilter> = _selectedFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(DownloadSortOrder.DATE_ADDED_DESC)
    val sortOrder: StateFlow<DownloadSortOrder> = _sortOrder.asStateFlow()

    private val _detectedClipboardUrl = MutableStateFlow<String?>(null)
    val detectedClipboardUrl: StateFlow<String?> = _detectedClipboardUrl.asStateFlow()

    private var lastDismissedClipboardUrl: String? = null

    val totalSpeed: StateFlow<Long> = repository.totalSpeedFlow

    val downloads: StateFlow<List<DownloadEntity>> = combine(
        repository.allDownloads,
        _searchQuery,
        _selectedFilter,
        _sortOrder
    ) { all, query, filter, sort ->
        all.filter { item ->
            // Filter by search query
            val matchesQuery = query.isBlank() ||
                    item.fileName.contains(query, ignoreCase = true) ||
                    item.url.contains(query, ignoreCase = true)

            // Filter by status tab
            val matchesFilter = when (filter) {
                DownloadFilter.ALL -> true
                DownloadFilter.ACTIVE -> item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PENDING
                DownloadFilter.COMPLETED -> item.status == DownloadStatus.COMPLETED
                DownloadFilter.PAUSED -> item.status == DownloadStatus.PAUSED
                DownloadFilter.FAILED -> item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED
            }

            matchesQuery && matchesFilter
        }.let { filtered ->
            // Sort
            when (sort) {
                DownloadSortOrder.DATE_ADDED_DESC -> filtered.sortedByDescending { it.createdAt }
                DownloadSortOrder.DATE_ADDED_ASC -> filtered.sortedBy { it.createdAt }
                DownloadSortOrder.NAME_ASC -> filtered.sortedBy { it.fileName.lowercase() }
                DownloadSortOrder.NAME_DESC -> filtered.sortedByDescending { it.fileName.lowercase() }
                DownloadSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.fileSize }
                DownloadSortOrder.SIZE_ASC -> filtered.sortedBy { it.fileSize }
                DownloadSortOrder.PROGRESS_DESC -> filtered.sortedByDescending { it.progress }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val summaryMetrics: StateFlow<HomeSummaryMetrics> = combine(
        repository.allDownloads,
        repository.totalSpeedFlow
    ) { all, speed ->
        HomeSummaryMetrics(
            activeCount = all.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING },
            completedCount = all.count { it.status == DownloadStatus.COMPLETED },
            pausedCount = all.count { it.status == DownloadStatus.PAUSED },
            failedCount = all.count { it.status == DownloadStatus.FAILED },
            totalBytesDownloaded = all.sumOf { it.downloadedBytes },
            globalSpeed = speed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeSummaryMetrics()
    )

    init {
        checkClipboard()
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: DownloadFilter) {
        _selectedFilter.value = filter
    }

    fun onSortOrderSelected(order: DownloadSortOrder) {
        _sortOrder.value = order
    }

    fun checkClipboard() {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            if (!prefs.clipboardDetection) return@launch

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@launch
            if (clipboard.hasPrimaryClip() &&
                clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
            ) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString()?.trim()
                if (!text.isNullOrBlank() &&
                    (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) &&
                    text != lastDismissedClipboardUrl
                ) {
                    _detectedClipboardUrl.value = text
                }
            }
        }
    }

    fun dismissClipboardBanner() {
        lastDismissedClipboardUrl = _detectedClipboardUrl.value
        _detectedClipboardUrl.value = null
    }

    suspend fun isDuplicate(url: String): Boolean {
        return repository.isDuplicateUrl(url)
    }

    fun addDownload(
        url: String,
        fileName: String?,
        priority: DownloadPriority,
        checksumType: String?,
        checksumValue: String?,
        speedLimit: Long
    ) {
        viewModelScope.launch {
            repository.enqueueDownload(
                url = url,
                suggestedFileName = fileName,
                priority = priority,
                checksumType = checksumType,
                checksumValue = checksumValue,
                speedLimit = speedLimit
            )
            dismissClipboardBanner()
        }
    }

    fun addBatchDownloads(urls: List<String>, priority: DownloadPriority) {
        viewModelScope.launch {
            repository.enqueueBatch(urls, priority)
        }
    }

    fun pauseDownload(id: Long) = repository.pauseDownload(id)

    fun resumeDownload(id: Long) = repository.resumeDownload(id)

    fun cancelDownload(id: Long) = repository.cancelDownload(id)

    fun retryDownload(id: Long) = repository.retryDownload(id)

    fun deleteDownload(id: Long, deleteFileFromStorage: Boolean) =
        repository.deleteDownload(id, deleteFileFromStorage)

    fun renameDownload(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renameDownload(id, newName)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            val all = repository.allDownloads.first()
            all.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
                .forEach { repository.pauseDownload(it.id) }
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            val all = repository.allDownloads.first()
            all.filter { it.status == DownloadStatus.PAUSED }
                .forEach { repository.resumeDownload(it.id) }
        }
    }

    companion object {
        fun provideFactory(
            repository: DownloadRepository,
            preferencesRepository: UserPreferencesRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repository, preferencesRepository, context) as T
            }
        }
    }
}
