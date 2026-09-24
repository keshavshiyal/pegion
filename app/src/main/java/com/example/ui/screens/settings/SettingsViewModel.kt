package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.UserPreferences
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiOnly(enabled) }
    }

    fun setChargingOnly(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setChargingOnly(enabled) }
    }

    fun setMaxConcurrent(count: Int) {
        viewModelScope.launch { preferencesRepository.setMaxConcurrent(count) }
    }

    fun setSpeedLimitKbps(kbps: Long) {
        viewModelScope.launch { preferencesRepository.setSpeedLimitKbps(kbps) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDownloadFolder(folder: String) {
        viewModelScope.launch { preferencesRepository.setDownloadFolder(folder) }
    }

    fun setClipboardDetection(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setClipboardDetection(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    suspend fun exportJson(): String {
        return preferencesRepository.exportJson(preferencesRepository.userPreferencesFlow.first())
    }

    suspend fun importJson(json: String): Boolean {
        return preferencesRepository.importJson(json)
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            downloadRepository.clearAll()
        }
    }

    companion object {
        fun provideFactory(
            preferencesRepository: UserPreferencesRepository,
            downloadRepository: DownloadRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(preferencesRepository, downloadRepository) as T
            }
        }
    }
}
