package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import com.example.download.worker.DownloadWorker
import java.util.concurrent.TimeUnit

class PegionApp : Application(), Configuration.Provider {

    lateinit var appContainer: AppContainer
        private set

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContainer = DefaultAppContainer(this)

        // Setup background periodic work for reliable background resume
        scheduleBackgroundWorker()

        // Trigger queue check on startup
        try {
            appContainer.downloadEngine.triggerQueueProcessing()
        } catch (e: Exception) {
            Log.w("PegionApp", "Queue processing deferral: ${e.message}")
        }
    }

    private fun scheduleBackgroundWorker() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<DownloadWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "pegion_download_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        } catch (e: Exception) {
            Log.w("PegionApp", "WorkManager schedule error (e.g. during test): ${e.message}")
        }
    }

    companion object {
        lateinit var instance: PegionApp
            private set
    }
}
