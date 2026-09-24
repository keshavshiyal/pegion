package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val speed: Long = 0L, // bytes per second
    val eta: Long = -1L, // seconds remaining
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val priority: DownloadPriority = DownloadPriority.NORMAL,
    val checksumType: String? = null, // e.g. "MD5" or "SHA-256"
    val checksumValue: String? = null, // expected hash
    val actualChecksum: String? = null, // computed hash after download
    val retryCount: Int = 0,
    val mimeType: String? = null,
    val speedLimit: Long = 0L, // 0 = use global limit or unlimited, else per-download bytes/sec
    val systemDownloadId: Long? = null // if delegated to system DownloadManager
)
