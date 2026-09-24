package com.example.download.model

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
