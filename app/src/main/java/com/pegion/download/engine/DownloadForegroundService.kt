package com.pegion.download.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.pegion.MainActivity
import com.pegion.PegionApp
import com.pegion.R
import com.pegion.data.local.entity.DownloadEntity
import com.pegion.download.model.DownloadStatus
import com.pegion.download.model.LiveDownloadStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
        startObservingDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = createServiceNotification("Pegion Download Engine", "Active", 0, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
        return START_STICKY
    }

    private fun startObservingDownloads() {
        observerJob?.cancel()
        val app = applicationContext as PegionApp
        val engine = app.appContainer.downloadEngine
        val dao = app.appContainer.database.downloadDao()
        val prefsRepo = app.appContainer.preferencesRepository
        val fileNameCache = ConcurrentHashMap<Long, String>()

        observerJob = serviceScope.launch {
            combine(
                engine.liveStatsFlow,
                prefsRepo.userPreferencesFlow
            ) { statsMap, prefs ->
                Pair(statsMap, prefs)
            }.collect { (statsMap, prefs) ->
                if (statsMap.isEmpty()) {
                    delay(2000)
                    if (engine.liveStatsFlow.value.isEmpty()) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else if (prefs.notificationsEnabled) {
                    for (downloadId in statsMap.keys) {
                        if (!fileNameCache.containsKey(downloadId)) {
                            dao.getDownloadByIdSync(downloadId)?.fileName?.let {
                                fileNameCache[downloadId] = it
                            }
                        }
                    }
                    updateActiveNotifications(statsMap.values.toList(), fileNameCache)
                }
            }
        }
    }

    private fun updateActiveNotifications(activeList: List<LiveDownloadStats>, nameMap: Map<Long, String>) {
        if (activeList.isEmpty()) return

        if (activeList.size == 1) {
            val item = activeList.first()
            val name = nameMap[item.downloadId] ?: "Downloading..."
            val notif = createSingleDownloadNotification(
                downloadId = item.downloadId,
                fileName = name,
                speed = item.speed,
                progress = item.progress,
                eta = item.eta,
                fileSize = item.fileSize
            )
            notificationManager.notify(NOTIFICATION_ID, notif)
        } else {
            val totalBytes = activeList.sumOf { if (it.fileSize > 0) it.fileSize else 0L }
            val downloaded = activeList.sumOf { it.downloadedBytes }
            val totalProgress = if (totalBytes > 0) ((downloaded.toDouble() / totalBytes) * 100).toInt() else 0
            val totalSpeed = activeList.sumOf { it.speed }

            val summaryNotif = createServiceNotification(
                title = "Downloading ${activeList.size} files",
                text = "${formatSpeed(totalSpeed)} • Overall $totalProgress%",
                progress = totalProgress,
                max = 100
            )
            notificationManager.notify(NOTIFICATION_ID, summaryNotif)

            for (item in activeList) {
                val name = nameMap[item.downloadId] ?: "Downloading..."
                val itemNotif = createSingleDownloadNotification(
                    downloadId = item.downloadId,
                    fileName = name,
                    speed = item.speed,
                    progress = item.progress,
                    eta = item.eta,
                    fileSize = item.fileSize
                )
                notificationManager.notify(item.downloadId.toInt() + 1000, itemNotif)
            }
        }
    }

    private fun createSingleDownloadNotification(
        downloadId: Long,
        fileName: String,
        speed: Long,
        progress: Float,
        eta: Long,
        fileSize: Long
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SELECT_DOWNLOAD_ID", downloadId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            downloadId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, DownloadNotificationReceiver::class.java).apply {
            action = "com.pegion.app.ACTION_PAUSE"
            putExtra("DOWNLOAD_ID", downloadId)
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            (downloadId * 10 + 1).toInt(),
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, DownloadNotificationReceiver::class.java).apply {
            action = "com.pegion.app.ACTION_CANCEL"
            putExtra("DOWNLOAD_ID", downloadId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            (downloadId * 10 + 2).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = formatSpeed(speed)
        val progressInt = progress.toInt()
        val etaText = if (eta > 0) "ETA ${formatDuration(eta)}" else ""
        val subtitle = if (etaText.isNotBlank()) "$speedText • $etaText" else speedText

        return NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(fileName)
            .setContentText(subtitle)
            .setContentIntent(openAppPendingIntent)
            .setProgress(100, progressInt, fileSize <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun createServiceNotification(
        title: String,
        text: String,
        progress: Int,
        max: Int
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(max, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                "Downloads in Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress bar and controls for active downloads"
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Download Completed",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when downloads finish or fail"
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completedChannel)
        }
    }

    override fun onDestroy() {
        observerJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 9001
        const val CHANNEL_PROGRESS = "pegion_progress_channel"
        const val CHANNEL_COMPLETED = "pegion_completed_channel"

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            context.stopService(intent)
        }

        private fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec >= 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / (1024.0 * 1024.0))
                bytesPerSec >= 1024 -> "%.1f KB/s".format(bytesPerSec / 1024.0)
                else -> "$bytesPerSec B/s"
            }
        }

        private fun formatDuration(seconds: Long): String {
            val m = seconds / 60
            val s = seconds % 60
            return if (m > 0) "${m}m ${s}s" else "${s}s"
        }
    }
}
