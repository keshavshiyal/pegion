package com.example.di

import android.content.Context
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.local.database.PegionDatabase
import com.example.data.repository.DownloadRepository
import com.example.data.repository.DownloadRepositoryImpl
import com.example.download.engine.DownloadEngine
import com.example.download.engine.NetworkMonitor

interface AppContainer {
    val context: Context
    val database: PegionDatabase
    val preferencesRepository: UserPreferencesRepository
    val networkMonitor: NetworkMonitor
    val downloadEngine: DownloadEngine
    val downloadRepository: DownloadRepository
}

class DefaultAppContainer(override val context: Context) : AppContainer {

    override val database: PegionDatabase by lazy {
        PegionDatabase.getInstance(context)
    }

    override val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    override val downloadEngine: DownloadEngine by lazy {
        DownloadEngine(
            context = context,
            downloadDao = database.downloadDao(),
            preferencesRepository = preferencesRepository,
            networkMonitor = networkMonitor
        )
    }

    override val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(
            context = context,
            downloadDao = database.downloadDao(),
            preferencesRepository = preferencesRepository,
            downloadEngine = downloadEngine
        )
    }
}
