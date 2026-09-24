package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.details.DetailsViewModel
import com.example.ui.screens.details.DownloadDetailsScreen
import com.example.ui.screens.downloads.DownloadsListScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel

sealed interface NavIconSource {
    data class Vector(val selected: ImageVector, val unselected: ImageVector) : NavIconSource
    data class Resource(val selectedResId: Int, val unselectedResId: Int) : NavIconSource
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val iconSource: NavIconSource
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "Home",
        iconSource = NavIconSource.Resource(
            selectedResId = R.drawable.ic_pegion_nav,
            unselectedResId = R.drawable.ic_pegion_nav_outlined
        )
    ),
    BottomNavItem(
        route = Screen.Downloads.route,
        label = "Queue",
        iconSource = NavIconSource.Vector(
            selected = Icons.Filled.Queue,
            unselected = Icons.Outlined.Queue
        )
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        label = "Settings",
        iconSource = NavIconSource.Vector(
            selected = Icons.Filled.Settings,
            unselected = Icons.Outlined.Settings
        )
    ),
    BottomNavItem(
        route = Screen.About.route,
        label = "About",
        iconSource = NavIconSource.Vector(
            selected = Icons.Filled.Info,
            unselected = Icons.Outlined.Info
        )
    )
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
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (!isDetailsScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
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
                                when (val source = item.iconSource) {
                                    is NavIconSource.Vector -> {
                                        Icon(
                                            imageVector = if (selected) source.selected else source.unselected,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    is NavIconSource.Resource -> {
                                        Icon(
                                            painter = painterResource(if (selected) source.selectedResId else source.unselectedResId),
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
