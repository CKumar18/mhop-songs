package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.AppContainer
import com.example.R
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DocumentImportScreen
import com.example.ui.screens.FavouritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SongEditorScreen
import com.example.ui.screens.SongReaderScreen
import com.example.ui.theme.MhopLogoFrameBlue
import com.example.ui.theme.MhopSongsTheme
import com.example.ui.theme.MhopWhite
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.MhopViewModelFactory
import com.example.ui.viewmodel.SongReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object SearchRoute

@Serializable
object FavouritesRoute

@Serializable
object SettingsRoute

@Serializable
data class SongReaderRoute(val songId: String)

@Serializable
object AdminDashboardRoute

@Serializable
data class SongEditorRoute(val songId: String? = null)

@Serializable
object DocumentImportRoute

private data class TopNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val routeObj: Any,
    val testTag: String,
)

@Composable
fun MhopAppShell(appContainer: AppContainer) {
    val factory = remember(appContainer) { MhopViewModelFactory(appContainer) }
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val adminViewModel: AdminViewModel = viewModel(factory = factory)

    val readingPreferences by libraryViewModel.readingPreferences.collectAsStateWithLifecycle()
    val adminSession by libraryViewModel.adminSession.collectAsStateWithLifecycle()
    val syncStatus by libraryViewModel.syncStatus.collectAsStateWithLifecycle()

    val languageCounts by libraryViewModel.languageCounts.collectAsStateWithLifecycle()
    val favouriteSongs by libraryViewModel.favouriteSongs.collectAsStateWithLifecycle()
    val recentlyViewedSongs by libraryViewModel.recentlyViewedSongs.collectAsStateWithLifecycle()
    val recentlyAddedSongs by libraryViewModel.recentlyAddedSongs.collectAsStateWithLifecycle()
    val filteredSongs by libraryViewModel.filteredSongs.collectAsStateWithLifecycle()
    val searchFilter by libraryViewModel.searchFilter.collectAsStateWithLifecycle()
    val categories by libraryViewModel.categories.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(650)
        showSplash = false
    }

    MhopSongsTheme(themeMode = readingPreferences.appThemeMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val isTopLevelRoute = currentDestination?.let { dest ->
            dest.hasRoute<HomeRoute>() ||
                dest.hasRoute<SearchRoute>() ||
                dest.hasRoute<FavouritesRoute>() ||
                dest.hasRoute<SettingsRoute>()
        } ?: true

        val navItems = buildList {
            add(TopNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, HomeRoute, "nav_home"))
            add(TopNavItem("Search", Icons.Filled.Search, Icons.Outlined.Search, SearchRoute, "nav_search"))
            add(TopNavItem("Favourites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, FavouritesRoute, "nav_favourites"))
            add(TopNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, SettingsRoute, "nav_settings"))
            if (adminSession.isAdmin) {
                add(
                    TopNavItem(
                        "Admin",
                        Icons.Filled.AdminPanelSettings,
                        Icons.Outlined.AdminPanelSettings,
                        AdminDashboardRoute,
                        "nav_admin",
                    )
                )
            }
        }

        fun navigateTopLevel(routeObj: Any) {
            navController.navigate(routeObj) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (isTopLevelRoute && !useNavigationRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.navigationBarsPadding(),
                            ) {
                                navItems.forEach { item ->
                                    val selected = when (item.routeObj) {
                                        is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                        is SearchRoute -> currentDestination?.hasRoute<SearchRoute>() == true
                                        is FavouritesRoute -> currentDestination?.hasRoute<FavouritesRoute>() == true
                                        is SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                        is AdminDashboardRoute -> currentDestination?.hasRoute<AdminDashboardRoute>() == true
                                        else -> false
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.routeObj) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                            )
                                        },
                                        label = { Text(item.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                        modifier = Modifier.testTag(item.testTag),
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        if (isTopLevelRoute && useNavigationRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxHeight(),
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))
                                navItems.forEach { item ->
                                    val selected = when (item.routeObj) {
                                        is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                                        is SearchRoute -> currentDestination?.hasRoute<SearchRoute>() == true
                                        is FavouritesRoute -> currentDestination?.hasRoute<FavouritesRoute>() == true
                                        is SettingsRoute -> currentDestination?.hasRoute<SettingsRoute>() == true
                                        is AdminDashboardRoute -> currentDestination?.hasRoute<AdminDashboardRoute>() == true
                                        else -> false
                                    }
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.routeObj) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                            )
                                        },
                                        label = { Text(item.label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                        modifier = Modifier.testTag(item.testTag),
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    languageCounts = languageCounts,
                                    favouriteCount = favouriteSongs.size,
                                    recentlyViewedSongs = recentlyViewedSongs,
                                    recentlyAddedSongs = recentlyAddedSongs,
                                    adminSession = adminSession,
                                    onOpenSearch = {
                                        libraryViewModel.clearFilters()
                                        navigateTopLevel(SearchRoute)
                                    },
                                    onSelectLanguage = { lang ->
                                        libraryViewModel.selectLanguageFilter(lang)
                                        navigateTopLevel(SearchRoute)
                                    },
                                    onOpenFavourites = { navigateTopLevel(FavouritesRoute) },
                                    onOpenSong = { songId -> navController.navigate(SongReaderRoute(songId)) },
                                    onToggleFavourite = { songId -> libraryViewModel.toggleFavourite(songId) },
                                    onOpenAdminDashboard = { navController.navigate(AdminDashboardRoute) },
                                    onOpenAdminSignIn = { navigateTopLevel(SettingsRoute) },
                                )
                            }

                            composable<SearchRoute> {
                                SearchScreen(
                                    songs = filteredSongs,
                                    filterState = searchFilter,
                                    categories = categories,
                                    onQueryChange = libraryViewModel::updateSearchQuery,
                                    onSelectLanguage = libraryViewModel::selectLanguageFilter,
                                    onSelectCategory = libraryViewModel::selectCategoryFilter,
                                    onSortOrderChange = libraryViewModel::updateSortOrder,
                                    onClearFilters = libraryViewModel::clearFilters,
                                    onOpenSong = { songId -> navController.navigate(SongReaderRoute(songId)) },
                                    onToggleFavourite = libraryViewModel::toggleFavourite,
                                )
                            }

                            composable<FavouritesRoute> {
                                FavouritesScreen(
                                    favouriteSongs = favouriteSongs,
                                    recentlyViewedSongs = recentlyViewedSongs,
                                    onOpenSong = { songId -> navController.navigate(SongReaderRoute(songId)) },
                                    onToggleFavourite = libraryViewModel::toggleFavourite,
                                    onBrowseSongs = { navigateTopLevel(SearchRoute) },
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    readingPreferences = readingPreferences,
                                    adminSession = adminSession,
                                    syncStatus = syncStatus,
                                    onUpdatePreferences = libraryViewModel::updateReadingPreferences,
                                    onResetPreferences = libraryViewModel::resetReadingPreferences,
                                    onSignInLocalAdmin = libraryViewModel::signInLocalAdminPreview,
                                    onSignOutAdmin = libraryViewModel::signOutAdmin,
                                    onOpenAdminDashboard = { navController.navigate(AdminDashboardRoute) },
                                )
                            }

                            composable<SongReaderRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<SongReaderRoute>()
                                val readerViewModel: SongReaderViewModel = viewModel(
                                    viewModelStoreOwner = backStackEntry,
                                    factory = factory,
                                )
                                SongReaderScreen(
                                    songId = route.songId,
                                    isAdmin = adminSession.isAdmin,
                                    viewModel = readerViewModel,
                                    onBack = { navController.popBackStack() },
                                    onEditSong = { id -> navController.navigate(SongEditorRoute(id)) },
                                )
                            }

                            composable<AdminDashboardRoute> {
                                AdminDashboardScreen(
                                    adminSession = adminSession,
                                    viewModel = adminViewModel,
                                    onBack = { navController.popBackStack() },
                                    onAddSong = { navController.navigate(SongEditorRoute(null)) },
                                    onEditSong = { id -> navController.navigate(SongEditorRoute(id)) },
                                    onPreviewSong = { id -> navController.navigate(SongReaderRoute(id)) },
                                    onOpenDocumentImport = {
                                        adminViewModel.resetImportWorkflow()
                                        navController.navigate(DocumentImportRoute)
                                    },
                                )
                            }

                            composable<SongEditorRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<SongEditorRoute>()
                                SongEditorScreen(
                                    songId = route.songId,
                                    viewModel = adminViewModel,
                                    onBack = { navController.popBackStack() },
                                    onPreviewSong = { savedId -> navController.navigate(SongReaderRoute(savedId)) },
                                )
                            }

                            composable<DocumentImportRoute> {
                                DocumentImportScreen(
                                    viewModel = adminViewModel,
                                    onBack = { navController.popBackStack() },
                                    onReviewExistingSong = { existingId ->
                                        navController.navigate(SongReaderRoute(existingId))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Branded Launch Splash Overlay using the exact logo colors
            AnimatedVisibility(
                visible = showSplash,
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MhopLogoFrameBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mhop_logo),
                            contentDescription = stringResource(R.string.logo_content_description),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(116.dp)
                                .clip(RoundedCornerShape(20.dp)),
                        )
                        Text(
                            text = "MHOP Songs",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MhopWhite,
                        )
                        Text(
                            text = "తెలుగు  |  हिन्दी  |  English",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MhopWhite.copy(alpha = 0.88f),
                        )
                    }
                }
            }
        }
    }
}
