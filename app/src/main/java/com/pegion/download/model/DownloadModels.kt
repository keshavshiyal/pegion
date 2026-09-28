package com.pegion.download.model

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class DownloadPriority(val weight: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3)
}

enum class ChecksumType {
    NONE,
    MD5,
    SHA_256
}

enum class DownloadFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    PAUSED,
    FAILED
}

enum class DownloadSortOrder {
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC,
    PROGRESS_DESC
}

data class SpeedSample(
    val timestamp: Long,
    val bytesPerSecond: Long
)

data class DownloadSegment(
    val segmentIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long,
    val isFinished: Boolean
) {
    val totalBytes: Long
        get() = (endByte - startByte + 1).coerceAtLeast(0L)

    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat() * 100f).coerceIn(0f, 100f) else 0f
}

data class LiveDownloadStats(
    val downloadId: Long,
    val downloadedBytes: Long,
    val fileSize: Long,
    val progress: Float,
    val speed: Long,
    val eta: Long,
    val segments: List<DownloadSegment> = emptyList()
)
