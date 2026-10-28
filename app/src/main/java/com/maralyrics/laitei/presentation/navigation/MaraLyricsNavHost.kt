package com.maralyrics.laitei.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maralyrics.laitei.presentation.articles.ArticleDetailScreen
import com.maralyrics.laitei.presentation.articles.ArticlesScreen
import com.maralyrics.laitei.presentation.calendar.CalendarScreen
import com.maralyrics.laitei.presentation.home.HomeScreen
import com.maralyrics.laitei.presentation.idol.IdolSeasonScreen
import com.maralyrics.laitei.presentation.idol.MaraIdolScreen
import com.maralyrics.laitei.presentation.onboarding.OnboardingScreen
import com.maralyrics.laitei.presentation.settings.SettingsScreen
import com.maralyrics.laitei.presentation.settings.CreditsScreen
import com.maralyrics.laitei.presentation.setup.SetupScreen
import com.maralyrics.laitei.presentation.song_detail.SongDetailScreen
import com.maralyrics.laitei.presentation.people.PeopleDirectoryScreen
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
            navController.navigate(SongRoute.create(pendingSongDeepLink)) {
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
                onSongClick = { songId, order ->
                    navController.navigate(SongRoute.create(songId, order))
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
                onArticlesClick = { navController.navigate("articles") },
                onMaraIdolClick = { navController.navigate("mara_idol") },
                onCalendarClick = { navController.navigate("calendar") },
                onPeopleClick = { navController.navigate("people") },
                focusSearchOnStart = pendingSearchDeepLink,
                onSearchFocusConsumed = onSearchDeepLinkConsumed
            )
        }

        composable("favorites") {
            saveRoute("favorites")
            FavoriteSongsScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songId, order -> navController.navigate(SongRoute.create(songId, order)) },
                onBrowseClick = { navController.navigate("home") }
            )
        }
        
        composable(
            route = SongRoute.PATTERN,
            arguments = listOf(
                navArgument(SongRoute.ARG_SONG_ID) { type = NavType.LongType },
                navArgument(SongRoute.ARG_ORDER) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong(SongRoute.ARG_SONG_ID)
            LaunchedEffect(songId) {
                if (songId != null) saveRoute(SongRoute.create(songId))
            }
            SongDetailScreen(
                // Previous/Next swap the song in place; keep the session pointing at the one on screen.
                onCurrentSongChanged = { currentId -> saveRoute(SongRoute.create(currentId)) },
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
                onSongClick = { songId, order -> navController.navigate(SongRoute.create(songId, order)) }
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
                onSongClick = { songId, order -> navController.navigate(SongRoute.create(songId, order)) }
            )
        }
        
        // Website sections. These deliberately don't call saveRoute: a relaunch resumes on the
        // last song/list screen, not on a news or calendar page.
        composable("articles") {
            ArticlesScreen(
                onBackClick = { navController.popBackStack() },
                onArticleClick = { slug -> navController.navigate("article/${Uri.encode(slug)}") }
            )
        }

        composable(
            route = "article/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) {
            ArticleDetailScreen(onBackClick = { navController.popBackStack() })
        }

        composable("mara_idol") {
            MaraIdolScreen(
                onBackClick = { navController.popBackStack() },
                onSeasonClick = { slug -> navController.navigate("mara_idol/${Uri.encode(slug)}") }
            )
        }

        composable(
            route = "mara_idol/{season}",
            arguments = listOf(navArgument("season") { type = NavType.StringType })
        ) {
            IdolSeasonScreen(
                onBackClick = { navController.popBackStack() },
                onArtistClick = { slug ->
                    navController.navigate("profile_detail/${Uri.encode(slug)}/${ProfileType.ARTIST.name}")
                }
            )
        }

        composable("people") {
            PeopleDirectoryScreen(
                onBackClick = { navController.popBackStack() },
                onPersonClick = { slug, type ->
                    navController.navigate("profile_detail/${Uri.encode(slug)}/${type.name}")
                }
            )
        }

        composable("calendar") {
            CalendarScreen(onBackClick = { navController.popBackStack() })
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

