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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.pegion.MainActivity
import com.pegion.PegionApp
import com.pegion.R
import com.pegion.download.model.LiveDownloadStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observerJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val postedNotificationIds = ConcurrentHashMap.newKeySet<Int>()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pegion::DownloadServiceWakeLock").apply {
            setReferenceCounted(false)
        }
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
        acquireWakeLock()
        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
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
                dao.getActiveDownloads(),
                prefsRepo.userPreferencesFlow
            ) { statsMap, activeEntities, prefs ->
                Triple(statsMap, activeEntities, prefs)
            }.collect { (statsMap, activeEntities, prefs) ->
                val hasActive = statsMap.isNotEmpty() || activeEntities.isNotEmpty()

                if (!hasActive) {
                    delay(3500)
                    val stillActive = engine.liveStatsFlow.value.isNotEmpty() || dao.getActiveDownloadsSync().isNotEmpty()
                    if (!stillActive) {
                        clearAllActiveNotifications()
                        releaseWakeLock()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                }

                // Keep device awake while there are downloads
                acquireWakeLock()

                if (!prefs.notificationsEnabled) {
                    clearAllActiveNotifications()
                    return@collect
                }

                for (item in activeEntities) {
                    fileNameCache[item.id] = item.fileName
                }

                if (statsMap.isNotEmpty()) {
                    updateActiveNotifications(statsMap.values.toList(), fileNameCache)
                } else if (activeEntities.isNotEmpty()) {
                    val firstEntity = activeEntities.first()
                    val notif = createServiceNotification(
                        title = "Pegion Download Engine",
                        text = "Preparing ${firstEntity.fileName}…",
                        progress = 0,
                        max = 0
                    )
                    notificationManager.notify(NOTIFICATION_ID, notif)
                    clearIndividualNotifications()
                }
            }
        }
    }

    private fun clearAllActiveNotifications() {
        for (id in postedNotificationIds) {
            notificationManager.cancel(id)
        }
        postedNotificationIds.clear()
    }

    private fun clearIndividualNotifications() {
        for (id in postedNotificationIds) {
            notificationManager.cancel(id)
        }
        postedNotificationIds.clear()
    }

    private fun updateActiveNotifications(activeList: List<LiveDownloadStats>, nameMap: Map<Long, String>) {
        if (activeList.isEmpty()) {
            clearIndividualNotifications()
            return
        }

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
            clearIndividualNotifications()
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

            val currentIds = activeList.map { (it.downloadId.toInt() + 1000) }.toSet()
            // Dismiss stale notifications of items that completed or were removed
            val staleIds = postedNotificationIds.filter { it !in currentIds }
            for (staleId in staleIds) {
                notificationManager.cancel(staleId)
                postedNotificationIds.remove(staleId)
            }

            for (item in activeList) {
                val name = nameMap[item.downloadId] ?: "Downloading..."
                val notifId = item.downloadId.toInt() + 1000
                val itemNotif = createSingleDownloadNotification(
                    downloadId = item.downloadId,
                    fileName = name,
                    speed = item.speed,
                    progress = item.progress,
                    eta = item.eta,
                    fileSize = item.fileSize
                )
                notificationManager.notify(notifId, itemNotif)
                postedNotificationIds.add(notifId)
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
            .setProgress(max, progress, max <= 0)
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
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 9001
        const val CHANNEL_PROGRESS = "pegion_progress_channel"
        const val CHANNEL_COMPLETED = "pegion_completed_channel"

        fun start(context: Context) {
            try {
                val intent = Intent(context, DownloadForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DownloadForegroundService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }

        fun cancelDownloadNotification(context: Context, downloadId: Long) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(downloadId.toInt() + 1000)
            } catch (_: Exception) {}
        }

        fun sendCompletionNotification(
            context: Context,
            downloadId: Long,
            fileName: String,
            filePath: String,
            mimeType: String?
        ) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                cancelDownloadNotification(context, downloadId)

                val file = File(filePath)
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }

                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType ?: "*/*")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val pendingOpen = PendingIntent.getActivity(
                    context,
                    downloadId.toInt() + 5000,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notif = NotificationCompat.Builder(context, CHANNEL_COMPLETED)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Download Complete")
                    .setContentText(fileName)
                    .setContentIntent(pendingOpen)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                nm.notify(downloadId.toInt() + 2000, notif)
            } catch (_: Exception) {}
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
