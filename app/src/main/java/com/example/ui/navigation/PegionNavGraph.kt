package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.details.DetailsViewModel
import com.example.ui.screens.details.DownloadDetailsScreen
import com.example.ui.screens.downloads.DownloadsListScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload),
    BottomNavItem(Screen.Downloads.route, "Queue", Icons.Filled.Queue, Icons.Outlined.Queue),
    BottomNavItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    BottomNavItem(Screen.About.route, "About", Icons.Filled.Info, Icons.Outlined.Info)
)

@Composable
fun PegionNavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    getDetailsViewModel: (Long) -> DetailsViewModel,
    sharedUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val isDetailsScreen = currentDestination?.startsWith("details/") == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isDetailsScreen) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToDetails = { downloadId ->
                        navController.navigate(Screen.Details.createRoute(downloadId))
                    },
                    sharedUrl = sharedUrl
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsListScreen(
                    viewModel = homeViewModel,
                    onNavigateToDetails = { downloadId ->
                        navController.navigate(Screen.Details.createRoute(downloadId))
                    }
                )
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("downloadId") { type = NavType.LongType })
            ) { backStackEntry ->
                val downloadId = backStackEntry.arguments?.getLong("downloadId") ?: -1L
                val detailsViewModel = getDetailsViewModel(downloadId)
                DownloadDetailsScreen(
                    viewModel = detailsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }

            composable(Screen.About.route) {
                AboutScreen()
            }
        }
    }
}
