package com.example.download.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.example.data.local.dao.DownloadDao
import com.example.data.local.entity.DownloadEntity
import com.example.data.datastore.UserPreferencesRepository
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadStatus
import com.example.download.model.SpeedSample
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadEngine(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor
) {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val speedHistoryMap = ConcurrentHashMap<Long, MutableList<SpeedSample>>()

    private val _totalSpeedFlow = MutableStateFlow(0L)
    val totalSpeedFlow: StateFlow<Long> = _totalSpeedFlow.asStateFlow()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    init {
        // Auto-pause / resume based on network & battery conditions
        engineScope.launch {
            networkMonitor.conditionsFlow.collect { conditions ->
                checkConditionsAndAdjust(conditions)
            }
        }
    }

    private suspend fun checkConditionsAndAdjust(conditions: DeviceConditions) {
        val prefs = preferencesRepository.userPreferencesFlow.first()
        val shouldPause = (!conditions.isConnected) ||
                (prefs.wifiOnly && !conditions.isWifi) ||
                (prefs.chargingOnly && !conditions.isCharging)

        if (shouldPause) {
            // Pause all currently running downloads
            val downloading = downloadDao.getCurrentlyDownloadingSync()
            for (item in downloading) {
                pauseDownload(item.id)
            }
        } else {
            // Check if we can schedule pending downloads
            triggerQueueProcessing()
        }
    }

    fun getSpeedHistory(downloadId: Long): List<SpeedSample> {
        return speedHistoryMap[downloadId]?.toList() ?: emptyList()
    }

    fun startDownload(downloadId: Long) {
        if (activeJobs.containsKey(downloadId)) return

        val job = engineScope.launch {
            processDownload(downloadId)
        }
        activeJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        engineScope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
            updateTotalSpeed()
            triggerQueueProcessing()
        }
    }

    fun resumeDownload(downloadId: Long) {
        engineScope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PENDING)
            triggerQueueProcessing()
        }
    }

    fun cancelDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        engineScope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
            updateTotalSpeed()
            triggerQueueProcessing()
        }
    }

    fun retryDownload(downloadId: Long) {
        engineScope.launch {
            val entity = downloadDao.getDownloadByIdSync(downloadId) ?: return@launch
            // Reset bytes and progress if it had failed
            val file = File(entity.filePath)
            if (file.exists()) {
                file.delete()
            }
            downloadDao.updateProgress(
                id = downloadId,
                downloadedBytes = 0L,
                fileSize = entity.fileSize,
                progress = 0f,
                speed = 0L,
                eta = -1L
            )
            downloadDao.updateStatus(downloadId, DownloadStatus.PENDING)
            triggerQueueProcessing()
        }
    }

    fun deleteDownload(downloadId: Long, deleteFileFromStorage: Boolean) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        engineScope.launch {
            val entity = downloadDao.getDownloadByIdSync(downloadId)
            if (deleteFileFromStorage && entity != null) {
                try {
                    val file = File(entity.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            downloadDao.deleteById(downloadId)
            speedHistoryMap.remove(downloadId)
            updateTotalSpeed()
            triggerQueueProcessing()
        }
    }

    fun triggerQueueProcessing() {
        engineScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val conditions = DeviceConditions(
                isConnected = networkMonitor.isCurrentlyConnected(),
                isWifi = networkMonitor.isCurrentlyWifi(),
                isCharging = networkMonitor.isCurrentlyCharging()
            )

            if (!conditions.isConnected) return@launch
            if (prefs.wifiOnly && !conditions.isWifi) return@launch
            if (prefs.chargingOnly && !conditions.isCharging) return@launch

            val maxConcurrent = prefs.maxConcurrent
            val currentDownloading = downloadDao.getCurrentlyDownloadingSync()
            val availableSlots = maxConcurrent - currentDownloading.size

            if (availableSlots > 0) {
                val pendingList = downloadDao.getDownloadsByStatusSync(DownloadStatus.PENDING)
                for (pending in pendingList.take(availableSlots)) {
                    startDownload(pending.id)
                }
            }
        }
    }

    private suspend fun processDownload(downloadId: Long) = withContext(Dispatchers.IO) {
        var entity = downloadDao.getDownloadByIdSync(downloadId) ?: return@withContext
        val prefs = preferencesRepository.userPreferencesFlow.first()

        // Setup destination file
        val targetFile = File(entity.filePath)
        targetFile.parentFile?.mkdirs()

        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING)
        DownloadForegroundService.start(context)

        // Effective speed limit: per-download if > 0, else global if > 0
        val effectiveLimitBytesPerSec = when {
            entity.speedLimit > 0 -> entity.speedLimit
            prefs.speedLimitKbps > 0 -> prefs.speedLimitKbps * 1024
            else -> 0L
        }
        val speedLimiter = SpeedLimiter(effectiveLimitBytesPerSec)

        val requestBuilder = Request.Builder()
            .url(entity.url)
            .header("User-Agent", "Pegion/1.0 (Android; Always delivers)")

        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        var speedHistory = speedHistoryMap.getOrPut(downloadId) { mutableListOf() }

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                downloadDao.updateStatus(
                    downloadId,
                    DownloadStatus.FAILED,
                    "HTTP Error ${response.code}: ${response.message}"
                )
                activeJobs.remove(downloadId)
                updateTotalSpeed()
                triggerQueueProcessing()
                return@withContext
            }

            val body = response.body
            if (body == null) {
                downloadDao.updateStatus(downloadId, DownloadStatus.FAILED, "Empty response body")
                activeJobs.remove(downloadId)
                updateTotalSpeed()
                triggerQueueProcessing()
                return@withContext
            }

            val isPartial = response.code == 206
            val contentLength = body.contentLength()
            val totalBytes = if (isPartial) {
                existingBytes + contentLength
            } else {
                contentLength
            }

            // Immediately persist recognized file size from HTTP headers into database
            if (totalBytes > 0) {
                downloadDao.updateProgress(
                    id = downloadId,
                    downloadedBytes = existingBytes,
                    fileSize = totalBytes,
                    progress = if (totalBytes > 0) (existingBytes.toFloat() / totalBytes * 100f).coerceIn(0f, 100f) else 0f,
                    speed = 0L,
                    eta = -1L
                )
            }

            // Sniff mime type if not present
            val mimeType = response.header("Content-Type") ?: entity.mimeType

            val randomAccessFile = RandomAccessFile(targetFile, "rw")
            if (isPartial) {
                randomAccessFile.seek(existingBytes)
            } else {
                randomAccessFile.setLength(0)
                randomAccessFile.seek(0)
            }

            val inputStream: InputStream = body.byteStream()
            val buffer = ByteArray(32768) // 32KB buffer
            var bytesRead: Int
            var totalDownloaded = if (isPartial) existingBytes else 0L

            var lastSampleTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeed = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                // Check for cancellation
                if (!activeJobs.containsKey(downloadId)) {
                    randomAccessFile.close()
                    inputStream.close()
                    return@withContext
                }

                randomAccessFile.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                bytesSinceLastSample += bytesRead

                if (effectiveLimitBytesPerSec > 0) {
                    speedLimiter.throttle(bytesRead)
                }

                val now = System.currentTimeMillis()
                val elapsed = now - lastSampleTime
                if (elapsed >= 1000) {
                    currentSpeed = (bytesSinceLastSample * 1000) / elapsed
                    val eta = if (currentSpeed > 0 && totalBytes > 0) {
                        ((totalBytes - totalDownloaded) / currentSpeed).coerceAtLeast(0L)
                    } else {
                        -1L
                    }

                    val progress = if (totalBytes > 0) {
                        (totalDownloaded.toFloat() / totalBytes.toFloat()) * 100f
                    } else {
                        0f
                    }

                    downloadDao.updateProgress(
                        id = downloadId,
                        downloadedBytes = totalDownloaded,
                        fileSize = totalBytes,
                        progress = progress.coerceIn(0f, 100f),
                        speed = currentSpeed,
                        eta = eta
                    )

                    // Keep last 30 samples for speed chart
                    speedHistory.add(SpeedSample(now, currentSpeed))
                    if (speedHistory.size > 30) {
                        speedHistory.removeAt(0)
                    }

                    lastSampleTime = now
                    bytesSinceLastSample = 0L
                    updateTotalSpeed()
                }
            }

            randomAccessFile.close()
            inputStream.close()

            // Resolve true final file size from disk or stream
            val actualDiskLength = if (targetFile.exists()) targetFile.length() else totalDownloaded
            val finalFileSize = when {
                totalBytes > 0 -> totalBytes
                actualDiskLength > 0 -> actualDiskLength
                else -> totalDownloaded
            }
            val finalDownloadedBytes = if (actualDiskLength > 0) actualDiskLength else totalDownloaded

            // Verify checksum if specified
            var checksumMatches = true
            var actualChecksum: String? = null

            if (!entity.checksumType.isNullOrBlank() && !entity.checksumValue.isNullOrBlank()) {
                val algo = if (entity.checksumType.equals("SHA-256", ignoreCase = true)) "SHA-256" else "MD5"
                actualChecksum = ChecksumVerifier.calculateChecksum(targetFile, algo)
                checksumMatches = actualChecksum.equals(entity.checksumValue.trim(), ignoreCase = true)
            }

            if (!checksumMatches) {
                downloadDao.updateStatus(
                    downloadId,
                    DownloadStatus.FAILED,
                    "Checksum mismatch! Expected: ${entity.checksumValue}, got: $actualChecksum"
                )
            } else {
                downloadDao.markCompleted(
                    id = downloadId,
                    completedAt = System.currentTimeMillis(),
                    actualChecksum = actualChecksum,
                    downloadedBytes = finalDownloadedBytes,
                    fileSize = finalFileSize
                )
            }

            activeJobs.remove(downloadId)
            updateTotalSpeed()
            triggerQueueProcessing()

        } catch (e: CancellationException) {
            // Cancelled or paused
            activeJobs.remove(downloadId)
            updateTotalSpeed()
        } catch (e: Exception) {
            downloadDao.updateStatus(
                downloadId,
                DownloadStatus.FAILED,
                e.localizedMessage ?: "Unknown download failure"
            )
            activeJobs.remove(downloadId)
            updateTotalSpeed()
            triggerQueueProcessing()
        }
    }

    private suspend fun updateTotalSpeed() {
        val downloading = downloadDao.getCurrentlyDownloadingSync()
        val total = downloading.sumOf { it.speed }
        _totalSpeedFlow.value = total
    }

    companion object {
        fun resolveFileName(url: String, suggestedName: String?): String {
            if (!suggestedName.isNullOrBlank()) {
                return suggestedName.trim()
            }
            return try {
                val uri = Uri.parse(url)
                val lastPath = uri.lastPathSegment
                if (!lastPath.isNullOrBlank() && lastPath.contains(".")) {
                    lastPath
                } else {
                    "download_${System.currentTimeMillis()}"
                }
            } catch (_: Exception) {
                "download_${System.currentTimeMillis()}"
            }
        }

        fun getDownloadDirectory(context: Context, folderName: String): File {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pegionDir = File(downloadsDir, folderName)
            if (!pegionDir.exists()) {
                pegionDir.mkdirs()
            }
            return if (pegionDir.canWrite()) {
                pegionDir
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), folderName).apply { mkdirs() }
            }
        }
    }
}
