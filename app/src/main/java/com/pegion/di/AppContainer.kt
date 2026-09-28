package com.pegion.di

import android.content.Context
import com.pegion.data.datastore.UserPreferencesRepository
import com.pegion.data.local.database.PegionDatabase
import com.pegion.data.repository.DownloadRepository
import com.pegion.data.repository.DownloadRepositoryImpl
import com.pegion.download.engine.DownloadEngine
import com.pegion.download.engine.NetworkMonitor

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
            downloadSegmentDao = database.downloadSegmentDao(),
            preferencesRepository = preferencesRepository,
            networkMonitor = networkMonitor
        )
    }

    override val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(
            context = context,
            downloadDao = database.downloadDao(),
            downloadSegmentDao = database.downloadSegmentDao(),
            preferencesRepository = preferencesRepository,
            downloadEngine = downloadEngine
        )
    }
}
