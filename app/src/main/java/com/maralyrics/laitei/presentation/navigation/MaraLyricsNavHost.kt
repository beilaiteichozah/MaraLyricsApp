package com.maralyrics.laitei.presentation.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maralyrics.laitei.presentation.home.HomeScreen
import com.maralyrics.laitei.presentation.onboarding.OnboardingScreen
import com.maralyrics.laitei.presentation.settings.SettingsScreen
import com.maralyrics.laitei.presentation.settings.CreditsScreen
import com.maralyrics.laitei.presentation.setup.SetupScreen
import com.maralyrics.laitei.presentation.song_detail.SongDetailScreen
import com.maralyrics.laitei.presentation.profile.ProfileDetailScreen
import com.maralyrics.laitei.presentation.category.CategorySongsScreen
import com.maralyrics.laitei.presentation.favorites.FavoriteSongsScreen
import com.maralyrics.laitei.domain.model.ProfileType

@Composable
fun MaraLyricsNavHost(
    isSetupComplete: Boolean,
    hasCompletedOnboarding: Boolean,
    privacyAccepted: Boolean,
    initialRoute: String? = null,
    onRouteChanged: (String?) -> Unit = {},
    pendingSongDeepLink: Long? = null,
    onSongDeepLinkConsumed: () -> Unit = {},
    pendingSearchDeepLink: Boolean = false,
    onSearchDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    // The base destination: onboarding -> setup -> home
    val startDestination = when {
        !hasCompletedOnboarding || !privacyAccepted -> "onboarding"
        !isSetupComplete -> "setup"
        else -> "home"
    }

    // Track if we are in the middle of a session restoration
    var isRestoring by remember { mutableStateOf(initialRoute != null && initialRoute != "home" && initialRoute != "onboarding") }

    // Natural back stack restoration: if we have an initial route deeper than home,
    // we start at home and immediately navigate to the target route.
    LaunchedEffect(isSetupComplete, hasCompletedOnboarding, privacyAccepted, initialRoute) {
        if (hasCompletedOnboarding && privacyAccepted && isSetupComplete && initialRoute != null && initialRoute != "home" && initialRoute != "onboarding") {
            navController.navigate(initialRoute) {
                // Ensure we don't have duplicate homes
                popUpTo("home") { inclusive = false }
            }
            // Restoration complete
            isRestoring = false
        }
    }

    // Wrap route changes to avoid saving intermediate routes during restoration
    val saveRoute: (String?) -> Unit = { route ->
        if (!isRestoring) {
            onRouteChanged(route)
        }
    }

    // A notification or home screen widget tap wants to jump straight to a specific
    // song or to the search field — takes priority over restoring the last session route.
    LaunchedEffect(isSetupComplete, hasCompletedOnboarding, privacyAccepted, pendingSongDeepLink) {
        if (hasCompletedOnboarding && privacyAccepted && isSetupComplete && pendingSongDeepLink != null) {
            navController.navigate("song_detail/$pendingSongDeepLink") {
                popUpTo("home") { inclusive = false }
            }
            onSongDeepLinkConsumed()
        }
    }

    // The search widget always wants the home screen showing (with the field focused),
    // regardless of whatever screen the last session left off on.
    LaunchedEffect(isSetupComplete, hasCompletedOnboarding, privacyAccepted, pendingSearchDeepLink) {
        if (hasCompletedOnboarding && privacyAccepted && isSetupComplete && pendingSearchDeepLink) {
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    val next = if (isSetupComplete) "home" else "setup"
                    navController.navigate(next) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("setup") {
            SetupScreen()
        }
        
        composable("home") {
            saveRoute("home")
            HomeScreen(
                onSongClick = { songId ->
                    navController.navigate("song_detail/$songId")
                },
                onArtistClick = { slug ->
                    navController.navigate("profile_detail/$slug/${ProfileType.ARTIST.name}")
                },
                onComposerClick = { slug ->
                    navController.navigate("profile_detail/$slug/${ProfileType.COMPOSER.name}")
                },
                onFavoritesClick = {
                    navController.navigate("favorites")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                focusSearchOnStart = pendingSearchDeepLink,
                onSearchFocusConsumed = onSearchDeepLinkConsumed
            )
        }

        composable("favorites") {
            saveRoute("favorites")
            FavoriteSongsScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songId -> navController.navigate("song_detail/$songId") },
                onBrowseClick = { navController.navigate("home") }
            )
        }
        
        composable(
            route = "song_detail/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong("songId")
            LaunchedEffect(songId) {
                if (songId != null) saveRoute("song_detail/$songId")
            }
            SongDetailScreen(
                onBackClick = { navController.popBackStack() },
                onArtistClick = { slug -> navController.navigate("profile_detail/$slug/${ProfileType.ARTIST.name}") },
                onComposerClick = { slug -> navController.navigate("profile_detail/$slug/${ProfileType.COMPOSER.name}") },
                onCategoryClick = { category -> navController.navigate("category_songs/$category") }
            )
        }
        
        composable(
            route = "profile_detail/{slug}/{type}",
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug")
            val type = backStackEntry.arguments?.getString("type")
            LaunchedEffect(slug, type) {
                if (slug != null && type != null) saveRoute("profile_detail/$slug/$type")
            }
            ProfileDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songId -> navController.navigate("song_detail/$songId") }
            )
        }

        composable(
            route = "category_songs/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")
            LaunchedEffect(category) {
                if (category != null) saveRoute("category_songs/$category")
            }
            CategorySongsScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songId -> navController.navigate("song_detail/$songId") }
            )
        }
        
        composable("settings") {
            saveRoute("settings")
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onCreditsClick = { navController.navigate("credits") }
            )
        }

        composable("credits") {
            CreditsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
    }
}

