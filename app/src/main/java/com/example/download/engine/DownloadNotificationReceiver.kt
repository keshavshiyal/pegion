package com.example.download.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PegionApp

class DownloadNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val downloadId = intent.getLongExtra("DOWNLOAD_ID", -1L)
        if (downloadId == -1L) return

        val app = context.applicationContext as? PegionApp ?: return
        val engine = app.appContainer.downloadEngine

        when (action) {
            "com.pegion.app.ACTION_PAUSE" -> engine.pauseDownload(downloadId)
            "com.pegion.app.ACTION_RESUME" -> engine.resumeDownload(downloadId)
            "com.pegion.app.ACTION_CANCEL" -> engine.cancelDownload(downloadId)
            "com.pegion.app.ACTION_RETRY" -> engine.retryDownload(downloadId)
        }
    }
}
