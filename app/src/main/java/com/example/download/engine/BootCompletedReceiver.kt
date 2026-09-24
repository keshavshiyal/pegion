package com.example.download.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PegionApp

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? PegionApp ?: return
            // Re-trigger queue processing to auto-resume pending and interrupted downloads
            app.appContainer.downloadEngine.triggerQueueProcessing()
        }
    }
}
