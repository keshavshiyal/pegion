package com.pegion.ui.screens.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pegion.PegionApp
import com.pegion.R
import com.pegion.data.datastore.UserPreferences
import com.pegion.ui.components.AddDownloadDialog
import com.pegion.ui.theme.AppThemeMode
import com.pegion.ui.theme.PegionTheme
import kotlinx.coroutines.launch

class ShareTargetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawUrl = extractUrlFromIntent(intent)
        if (rawUrl.isNullOrBlank()) {
            Toast.makeText(this, R.string.share_no_url, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val app = application as PegionApp
        val appContainer = app.appContainer

        setContent {
            val prefs by appContainer.preferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )
            val appThemeMode = when (prefs.themeMode) {
                "LIGHT" -> AppThemeMode.LIGHT
                "DARK" -> AppThemeMode.DARK
                "AMOLED" -> AppThemeMode.AMOLED
                else -> AppThemeMode.SYSTEM
            }

            PegionTheme(themeMode = appThemeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AddDownloadDialog(
                            initialUrl = rawUrl,
                            onDismiss = { finish() },
                            onConfirm = { url, fileName, priority, checksumType, checksumValue, speedLimit ->
                                lifecycleScope.launch {
                                    appContainer.downloadRepository.enqueueDownload(
                                        url = url,
                                        suggestedFileName = fileName,
                                        priority = priority,
                                        checksumType = checksumType,
                                        checksumValue = checksumValue,
                                        speedLimit = speedLimit
                                    )
                                    val displayName = fileName?.takeIf { it.isNotBlank() } ?: url.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "file"
                                    Toast.makeText(
                                        this@ShareTargetActivity,
                                        "Download queued: $displayName",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                            },
                            checkDuplicate = { appContainer.downloadRepository.isDuplicateUrl(it) },
                            probeUrl = { appContainer.downloadRepository.probeUrl(it) }
                        )
                    }
                }
            }
        }
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.dataString
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            ?: return null

        val trimmed = text.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        val urlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
        val match = urlRegex.find(trimmed)
        return match?.value
    }
}
