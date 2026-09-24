package com.example.download.engine

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
import com.example.MainActivity
import com.example.PegionApp
import com.example.R
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

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
        val dao = app.appContainer.database.downloadDao()
        val prefsRepo = app.appContainer.preferencesRepository

        observerJob = serviceScope.launch {
            while (true) {
                val prefs = prefsRepo.userPreferencesFlow.first()
                val activeDownloads = dao.getCurrentlyDownloadingSync()

                if (activeDownloads.isEmpty()) {
                    // No active downloads, stop foreground after a short grace period
                    delay(2000)
                    val stillEmpty = dao.getCurrentlyDownloadingSync().isEmpty()
                    if (stillEmpty) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        break
                    }
                } else if (prefs.notificationsEnabled) {
                    updateActiveNotifications(activeDownloads)
                }
                delay(1000)
            }
        }
    }

    private fun updateActiveNotifications(activeList: List<DownloadEntity>) {
        if (activeList.isEmpty()) return

        if (activeList.size == 1) {
            val item = activeList.first()
            val notif = createSingleDownloadNotification(item)
            notificationManager.notify(NOTIFICATION_ID, notif)
        } else {
            // Multiple downloads: summary notification + individual notifications
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
                val itemNotif = createSingleDownloadNotification(item)
                notificationManager.notify(item.id.toInt() + 1000, itemNotif)
            }
        }
    }

    private fun createSingleDownloadNotification(item: DownloadEntity): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SELECT_DOWNLOAD_ID", item.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            item.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, DownloadNotificationReceiver::class.java).apply {
            action = "com.pegion.app.ACTION_PAUSE"
            putExtra("DOWNLOAD_ID", item.id)
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            (item.id * 10 + 1).toInt(),
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, DownloadNotificationReceiver::class.java).apply {
            action = "com.pegion.app.ACTION_CANCEL"
            putExtra("DOWNLOAD_ID", item.id)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            this,
            (item.id * 10 + 2).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = formatSpeed(item.speed)
        val progressInt = item.progress.toInt()
        val etaText = if (item.eta > 0) "ETA ${formatDuration(item.eta)}" else ""
        val subtitle = "$speedText • $etaText"

        return NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(item.fileName)
            .setContentText(subtitle)
            .setContentIntent(openAppPendingIntent)
            .setProgress(100, progressInt, item.fileSize <= 0)
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
            .setSmallIcon(android.R.drawable.stat_sys_download)
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
