package com.pegion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.pegion.ui.navigation.PegionNavGraph
import com.pegion.ui.navigation.Screen
import com.pegion.ui.screens.details.DetailsViewModel
import com.pegion.ui.screens.home.HomeViewModel
import com.pegion.ui.screens.settings.SettingsViewModel
import com.pegion.ui.theme.AppThemeMode
import com.pegion.ui.theme.PegionTheme

class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)
    private var pendingDownloadId by mutableStateOf<Long?>(null)
    private var homeViewModelInstance: HomeViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        val app = application as PegionApp
        val appContainer = app.appContainer

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(
                    appContainer.preferencesRepository,
                    appContainer.downloadRepository
                )
            )

            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.provideFactory(
                    appContainer.downloadRepository,
                    appContainer.preferencesRepository,
                    this
                )
            )
            homeViewModelInstance = homeViewModel

            val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()

            val appThemeMode = when (preferences.themeMode) {
                "LIGHT" -> AppThemeMode.LIGHT
                "DARK" -> AppThemeMode.DARK
                "AMOLED" -> AppThemeMode.AMOLED
                else -> AppThemeMode.SYSTEM
            }

            // Notification permission request for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Granted or denied handled silently */ }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            PegionTheme(themeMode = appThemeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(pendingDownloadId) {
                        pendingDownloadId?.let { id ->
                            navController.navigate(Screen.Details.createRoute(id))
                            pendingDownloadId = null
                        }
                    }

                    PegionNavGraph(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        settingsViewModel = settingsViewModel,
                        getDetailsViewModel = { id ->
                            DetailsViewModel(id, appContainer.downloadRepository)
                        },
                        sharedUrl = sharedUrl
                    )
                }
            }
        }
    }

    private val clipListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        homeViewModelInstance?.checkClipboard()
    }

    override fun onResume() {
        super.onResume()
        try {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboard?.addPrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}
        homeViewModelInstance?.checkClipboard()
    }

    override fun onPause() {
        super.onPause()
        try {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboard?.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            homeViewModelInstance?.checkClipboard()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Check if opened from notification with download ID
        val selectId = intent.getLongExtra("SELECT_DOWNLOAD_ID", -1L)
        if (selectId != -1L) {
            pendingDownloadId = selectId
        }

        // Check for share intent
        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank() && (text.startsWith("http://", true) || text.startsWith("https://", true))) {
                sharedUrl = text.trim()
            }
        }
    }
}
