package com.pegion.download.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.pegion.data.datastore.UserPreferencesRepository
import com.pegion.data.local.dao.DownloadDao
import com.pegion.data.local.dao.DownloadSegmentDao
import com.pegion.data.local.entity.DownloadEntity
import com.pegion.data.local.entity.DownloadSegmentEntity
import com.pegion.download.model.DownloadSegment
import com.pegion.download.model.DownloadStatus
import com.pegion.download.model.LiveDownloadStats
import com.pegion.download.model.SpeedSample
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class DownloadEngine(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val downloadSegmentDao: DownloadSegmentDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor
) {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val speedHistoryMap = ConcurrentHashMap<Long, MutableList<SpeedSample>>()

    // In-memory decoupled live telemetry
    private val _liveStatsFlow = MutableStateFlow<Map<Long, LiveDownloadStats>>(emptyMap())
    val liveStatsFlow: StateFlow<Map<Long, LiveDownloadStats>> = _liveStatsFlow.asStateFlow()

    private val _totalSpeedFlow = MutableStateFlow(0L)
    val totalSpeedFlow: StateFlow<Long> = _totalSpeedFlow.asStateFlow()

    // Highly optimized OkHttp client with connection pooling and HTTP/2
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
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
            val downloading = downloadDao.getCurrentlyDownloadingSync()
            for (item in downloading) {
                pauseDownload(item.id)
            }
        } else {
            triggerQueueProcessing()
        }
    }

    fun getSpeedHistory(downloadId: Long): List<SpeedSample> {
        return speedHistoryMap[downloadId]?.toList() ?: emptyList()
    }

    fun hasActiveJobs(): Boolean = activeJobs.isNotEmpty()

    fun startDownload(downloadId: Long) {
        if (activeJobs.containsKey(downloadId)) return

        DownloadForegroundService.start(context)
        val job = engineScope.launch {
            processDownload(downloadId)
        }
        activeJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        removeLiveStats(downloadId)
        DownloadForegroundService.cancelDownloadNotification(context, downloadId)
        engineScope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
            updateTotalSpeed()
            triggerQueueProcessing()
        }
    }

    fun resumeDownload(downloadId: Long) {
        DownloadForegroundService.start(context)
        engineScope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PENDING)
            triggerQueueProcessing()
        }
    }

    fun cancelDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        removeLiveStats(downloadId)
        DownloadForegroundService.cancelDownloadNotification(context, downloadId)
        engineScope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
            updateTotalSpeed()
            triggerQueueProcessing()
        }
    }

    fun retryDownload(downloadId: Long) {
        DownloadForegroundService.start(context)
        engineScope.launch {
            val entity = downloadDao.getDownloadByIdSync(downloadId) ?: return@launch
            val file = File(entity.filePath)
            if (file.exists()) {
                file.delete()
            }
            downloadSegmentDao.deleteSegmentsForDownload(downloadId)
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
        removeLiveStats(downloadId)
        DownloadForegroundService.cancelDownloadNotification(context, downloadId)
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.cancel(downloadId.toInt() + 2000)
        } catch (_: Exception) {}
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
            downloadSegmentDao.deleteSegmentsForDownload(downloadId)
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

    private data class ProbeResult(
        val contentLength: Long,
        val supportsRanges: Boolean,
        val mimeType: String?
    )

    private fun probeServer(url: String, currentMime: String?): ProbeResult {
        var contentLength = -1L
        var supportsRanges = false
        var mimeType = currentMime

        // 1. Try HEAD request
        try {
            val headRequest = Request.Builder()
                .url(url)
                .header("User-Agent", "Pegion/1.0 (Android; Always delivers)")
                .head()
                .build()

            val headResponse = okHttpClient.newCall(headRequest).execute()
            if (headResponse.isSuccessful) {
                contentLength = headResponse.header("Content-Length")?.toLongOrNull() ?: -1L
                val acceptRanges = headResponse.header("Accept-Ranges")
                supportsRanges = acceptRanges?.equals("bytes", ignoreCase = true) == true
                mimeType = headResponse.header("Content-Type") ?: currentMime
                headResponse.close()
                if (contentLength > 0 && supportsRanges) {
                    return ProbeResult(contentLength, true, mimeType)
                }
            } else {
                headResponse.close()
            }
        } catch (_: Exception) {}

        // 2. Try 1-byte Range probe request
        try {
            val probeRequest = Request.Builder()
                .url(url)
                .header("User-Agent", "Pegion/1.0 (Android; Always delivers)")
                .header("Range", "bytes=0-0")
                .build()

            val probeResponse = okHttpClient.newCall(probeRequest).execute()
            if (probeResponse.code == 206) {
                supportsRanges = true
                val contentRange = probeResponse.header("Content-Range")
                if (contentRange != null && contentRange.contains("/")) {
                    val totalStr = contentRange.substringAfter("/")
                    contentLength = totalStr.toLongOrNull() ?: -1L
                }
                mimeType = probeResponse.header("Content-Type") ?: currentMime
            } else if (probeResponse.isSuccessful) {
                contentLength = probeResponse.body?.contentLength() ?: -1L
            }
            probeResponse.close()
        } catch (_: Exception) {}

        return ProbeResult(contentLength, supportsRanges, mimeType)
    }

    private suspend fun processDownload(downloadId: Long) = withContext(Dispatchers.IO) {
        val entity = downloadDao.getDownloadByIdSync(downloadId) ?: return@withContext
        val prefs = preferencesRepository.userPreferencesFlow.first()

        downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING)
        DownloadForegroundService.start(context)

        val effectiveLimitBytesPerSec = when {
            entity.speedLimit > 0 -> entity.speedLimit
            prefs.speedLimitKbps > 0 -> prefs.speedLimitKbps * 1024
            else -> 0L
        }
        val speedLimiter = SpeedLimiter(effectiveLimitBytesPerSec)

        // Setup destination file
        val targetFile = File(entity.filePath)
        targetFile.parentFile?.mkdirs()

        // Probe server capabilities
        val probe = probeServer(entity.url, entity.mimeType)
        val finalFileSize = if (probe.contentLength > 0) probe.contentLength else entity.fileSize

        // Multi-segment threshold: file >= 2MB and server supports Range requests
        val canUseMultiSegment = probe.supportsRanges && finalFileSize >= 2 * 1024 * 1024

        try {
            if (canUseMultiSegment) {
                processMultiSegmentDownload(
                    entity = entity,
                    targetFile = targetFile,
                    contentLength = finalFileSize,
                    effectiveLimitBytesPerSec = effectiveLimitBytesPerSec,
                    speedLimiter = speedLimiter
                )
            } else {
                processSingleStreamDownload(
                    entity = entity,
                    targetFile = targetFile,
                    knownContentLength = finalFileSize,
                    effectiveLimitBytesPerSec = effectiveLimitBytesPerSec,
                    speedLimiter = speedLimiter
                )
            }
        } catch (e: CancellationException) {
            activeJobs.remove(downloadId)
            removeLiveStats(downloadId)
            DownloadForegroundService.cancelDownloadNotification(context, downloadId)
            updateTotalSpeed()
        } catch (e: Exception) {
            downloadDao.updateStatus(
                downloadId,
                DownloadStatus.FAILED,
                e.localizedMessage ?: "Unknown download failure"
            )
            activeJobs.remove(downloadId)
            removeLiveStats(downloadId)
            DownloadForegroundService.cancelDownloadNotification(context, downloadId)
            updateTotalSpeed()
            triggerQueueProcessing()
        }
    }

    /**
     * Blazing-fast parallel multi-segment download engine (4 to 8 threads).
     */
    private suspend fun processMultiSegmentDownload(
        entity: DownloadEntity,
        targetFile: File,
        contentLength: Long,
        effectiveLimitBytesPerSec: Long,
        speedLimiter: SpeedLimiter
    ) = coroutineScope {
        val downloadId = entity.id

        // Pre-allocate destination file space
        RandomAccessFile(targetFile, "rw").use { raf ->
            if (raf.length() < contentLength) {
                raf.setLength(contentLength)
            }
        }

        // Determine optimal segment count
        val segmentCount = when {
            contentLength >= 100 * 1024 * 1024 -> 6
            contentLength >= 10 * 1024 * 1024 -> 4
            else -> 2
        }

        // Load existing segments or partition file
        var existingSegments = downloadSegmentDao.getSegmentsSync(downloadId)
        if (existingSegments.isEmpty() || existingSegments.size != segmentCount) {
            downloadSegmentDao.deleteSegmentsForDownload(downloadId)
            val segLength = contentLength / segmentCount
            val newSegments = (0 until segmentCount).map { i ->
                val start = i * segLength
                val end = if (i == segmentCount - 1) contentLength - 1 else (i + 1) * segLength - 1
                DownloadSegmentEntity(
                    downloadId = downloadId,
                    segmentIndex = i,
                    startByte = start,
                    endByte = end,
                    downloadedBytes = 0L,
                    isFinished = false
                )
            }
            downloadSegmentDao.insertSegments(newSegments)
            existingSegments = newSegments
        }

        // In-memory atomic segment trackers
        val liveSegments = ConcurrentHashMap<Int, DownloadSegment>()
        for (seg in existingSegments) {
            liveSegments[seg.segmentIndex] = DownloadSegment(
                segmentIndex = seg.segmentIndex,
                startByte = seg.startByte,
                endByte = seg.endByte,
                downloadedBytes = seg.downloadedBytes,
                isFinished = seg.isFinished
            )
        }

        val speedHistory = speedHistoryMap.getOrPut(downloadId) { mutableListOf() }
        val transferredSinceLastTick = AtomicLong(0L)
        var lastTickTime = System.currentTimeMillis()
        var lastDbPersistTime = System.currentTimeMillis()

        // Telemetry supervisor coroutine: updates in-memory stats every 1s, Room DB every 5s
        val telemetryJob = launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                val elapsed = (now - lastTickTime).coerceAtLeast(1L)
                val bytesSince = transferredSinceLastTick.getAndSet(0L)
                val currentSpeed = (bytesSince * 1000) / elapsed
                val totalDownloaded = liveSegments.values.sumOf { it.downloadedBytes }

                val eta = if (currentSpeed > 0 && contentLength > 0) {
                    ((contentLength - totalDownloaded) / currentSpeed).coerceAtLeast(0L)
                } else {
                    -1L
                }

                val progress = if (contentLength > 0) {
                    (totalDownloaded.toFloat() / contentLength.toFloat() * 100f).coerceIn(0f, 100f)
                } else {
                    0f
                }

                val segmentList = liveSegments.values.sortedBy { it.segmentIndex }

                // Hot in-memory update
                updateLiveStats(
                    LiveDownloadStats(
                        downloadId = downloadId,
                        downloadedBytes = totalDownloaded,
                        fileSize = contentLength,
                        progress = progress,
                        speed = currentSpeed,
                        eta = eta,
                        segments = segmentList
                    )
                )

                speedHistory.add(SpeedSample(now, currentSpeed))
                if (speedHistory.size > 30) speedHistory.removeAt(0)
                updateTotalSpeed()

                lastTickTime = now

                // Throttled persistence to Room (every 5 seconds) to avoid flash wear
                if (now - lastDbPersistTime >= 5000) {
                    downloadDao.updateProgress(
                        id = downloadId,
                        downloadedBytes = totalDownloaded,
                        fileSize = contentLength,
                        progress = progress,
                        speed = currentSpeed,
                        eta = eta
                    )
                    for (seg in segmentList) {
                        downloadSegmentDao.updateSegmentProgress(
                            downloadId = downloadId,
                            segmentIndex = seg.segmentIndex,
                            downloadedBytes = seg.downloadedBytes,
                            isFinished = seg.isFinished
                        )
                    }
                    lastDbPersistTime = now
                }
            }
        }

        // Parallel segment download workers
        val workers = existingSegments.map { segEntity ->
            async(Dispatchers.IO) {
                if (segEntity.isFinished) return@async

                val currentOffset = segEntity.startByte + segEntity.downloadedBytes
                if (currentOffset > segEntity.endByte) {
                    liveSegments[segEntity.segmentIndex] = liveSegments[segEntity.segmentIndex]!!.copy(isFinished = true)
                    return@async
                }

                val segRequest = Request.Builder()
                    .url(entity.url)
                    .header("User-Agent", "Pegion/1.0 (Android; Always delivers)")
                    .header("Range", "bytes=$currentOffset-${segEntity.endByte}")
                    .build()

                val response = okHttpClient.newCall(segRequest).execute()
                if (!response.isSuccessful && response.code != 206) {
                    response.close()
                    throw java.io.IOException("Segment ${segEntity.segmentIndex} failed with HTTP ${response.code}")
                }

                val body = response.body ?: throw java.io.IOException("Empty segment body")
                val inputStream: InputStream = body.byteStream()
                val buffer = ByteArray(64 * 1024) // 64KB buffer for high throughput

                RandomAccessFile(targetFile, "rw").use { raf ->
                    raf.seek(currentOffset)
                    var bytesRead: Int
                    var segDownloaded = segEntity.downloadedBytes

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!activeJobs.containsKey(downloadId)) {
                            break
                        }

                        raf.write(buffer, 0, bytesRead)
                        segDownloaded += bytesRead
                        transferredSinceLastTick.addAndGet(bytesRead.toLong())

                        if (effectiveLimitBytesPerSec > 0) {
                            speedLimiter.throttle(bytesRead)
                        }

                        val isFinished = segDownloaded >= (segEntity.endByte - segEntity.startByte + 1)
                        liveSegments[segEntity.segmentIndex] = DownloadSegment(
                            segmentIndex = segEntity.segmentIndex,
                            startByte = segEntity.startByte,
                            endByte = segEntity.endByte,
                            downloadedBytes = segDownloaded,
                            isFinished = isFinished
                        )
                    }
                }
                inputStream.close()
                response.close()
            }
        }

        // Await all parallel segments to complete
        workers.awaitAll()
        telemetryJob.cancel()

        // Check if user paused or cancelled
        if (!activeJobs.containsKey(downloadId)) {
            return@coroutineScope
        }

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
            DownloadForegroundService.cancelDownloadNotification(context, downloadId)
        } else {
            downloadDao.markCompleted(
                id = downloadId,
                completedAt = System.currentTimeMillis(),
                actualChecksum = actualChecksum,
                downloadedBytes = contentLength,
                fileSize = contentLength
            )
            downloadSegmentDao.deleteSegmentsForDownload(downloadId)
            DownloadForegroundService.sendCompletionNotification(
                context = context,
                downloadId = downloadId,
                fileName = entity.fileName,
                filePath = entity.filePath,
                mimeType = entity.mimeType
            )
        }

        activeJobs.remove(downloadId)
        removeLiveStats(downloadId)
        updateTotalSpeed()
        triggerQueueProcessing()
    }

    /**
     * Single stream download engine for servers that do not support Range chunking.
     */
    private suspend fun processSingleStreamDownload(
        entity: DownloadEntity,
        targetFile: File,
        knownContentLength: Long,
        effectiveLimitBytesPerSec: Long,
        speedLimiter: SpeedLimiter
    ) = coroutineScope {
        val downloadId = entity.id
        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        val requestBuilder = Request.Builder()
            .url(entity.url)
            .header("User-Agent", "Pegion/1.0 (Android; Always delivers)")

        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful && response.code != 206) {
            downloadDao.updateStatus(
                downloadId,
                DownloadStatus.FAILED,
                "HTTP Error ${response.code}: ${response.message}"
            )
            activeJobs.remove(downloadId)
            removeLiveStats(downloadId)
            updateTotalSpeed()
            triggerQueueProcessing()
            return@coroutineScope
        }

        val body = response.body
        if (body == null) {
            downloadDao.updateStatus(downloadId, DownloadStatus.FAILED, "Empty response body")
            activeJobs.remove(downloadId)
            removeLiveStats(downloadId)
            updateTotalSpeed()
            triggerQueueProcessing()
            return@coroutineScope
        }

        val isPartial = response.code == 206
        val streamContentLength = body.contentLength()
        val totalBytes = when {
            isPartial -> existingBytes + streamContentLength
            streamContentLength > 0 -> streamContentLength
            knownContentLength > 0 -> knownContentLength
            else -> -1L
        }

        val randomAccessFile = RandomAccessFile(targetFile, "rw")
        if (isPartial) {
            randomAccessFile.seek(existingBytes)
        } else {
            randomAccessFile.setLength(0)
            randomAccessFile.seek(0)
        }

        val inputStream: InputStream = body.byteStream()
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        var totalDownloaded = if (isPartial) existingBytes else 0L

        var lastSampleTime = System.currentTimeMillis()
        var bytesSinceLastSample = 0L
        var currentSpeed = 0L
        var lastDbPersistTime = System.currentTimeMillis()
        val speedHistory = speedHistoryMap.getOrPut(downloadId) { mutableListOf() }

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            if (!activeJobs.containsKey(downloadId)) {
                randomAccessFile.close()
                inputStream.close()
                response.close()
                return@coroutineScope
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
                    (totalDownloaded.toFloat() / totalBytes.toFloat() * 100f).coerceIn(0f, 100f)
                } else {
                    0f
                }

                updateLiveStats(
                    LiveDownloadStats(
                        downloadId = downloadId,
                        downloadedBytes = totalDownloaded,
                        fileSize = totalBytes,
                        progress = progress,
                        speed = currentSpeed,
                        eta = eta
                    )
                )

                speedHistory.add(SpeedSample(now, currentSpeed))
                if (speedHistory.size > 30) speedHistory.removeAt(0)
                updateTotalSpeed()

                lastSampleTime = now
                bytesSinceLastSample = 0L

                // Persist to Room periodically
                if (now - lastDbPersistTime >= 5000) {
                    downloadDao.updateProgress(
                        id = downloadId,
                        downloadedBytes = totalDownloaded,
                        fileSize = totalBytes,
                        progress = progress,
                        speed = currentSpeed,
                        eta = eta
                    )
                    lastDbPersistTime = now
                }
            }
        }

        randomAccessFile.close()
        inputStream.close()
        response.close()

        val actualDiskLength = if (targetFile.exists()) targetFile.length() else totalDownloaded
        val finalFileSize = when {
            totalBytes > 0 -> totalBytes
            actualDiskLength > 0 -> actualDiskLength
            else -> totalDownloaded
        }

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
            DownloadForegroundService.cancelDownloadNotification(context, downloadId)
        } else {
            downloadDao.markCompleted(
                id = downloadId,
                completedAt = System.currentTimeMillis(),
                actualChecksum = actualChecksum,
                downloadedBytes = actualDiskLength,
                fileSize = finalFileSize
            )
            DownloadForegroundService.sendCompletionNotification(
                context = context,
                downloadId = downloadId,
                fileName = entity.fileName,
                filePath = entity.filePath,
                mimeType = entity.mimeType
            )
        }

        activeJobs.remove(downloadId)
        removeLiveStats(downloadId)
        updateTotalSpeed()
        triggerQueueProcessing()
    }

    private fun updateLiveStats(stats: LiveDownloadStats) {
        val current = _liveStatsFlow.value.toMutableMap()
        current[stats.downloadId] = stats
        _liveStatsFlow.value = current
    }

    private fun removeLiveStats(downloadId: Long) {
        val current = _liveStatsFlow.value.toMutableMap()
        current.remove(downloadId)
        _liveStatsFlow.value = current
    }

    private fun updateTotalSpeed() {
        val liveSpeed = _liveStatsFlow.value.values.sumOf { it.speed }
        _totalSpeedFlow.value = liveSpeed
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
