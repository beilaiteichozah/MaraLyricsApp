package com.maralyrics.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maralyrics.presentation.home.HomeScreen
import com.maralyrics.presentation.settings.SettingsScreen
import com.maralyrics.presentation.setup.SetupScreen
import com.maralyrics.presentation.song_detail.SongDetailScreen
import com.maralyrics.presentation.profile.ProfileDetailScreen
import com.maralyrics.presentation.category.CategorySongsScreen
import com.maralyrics.domain.model.ProfileType

@Composable
fun MaraLyricsNavHost(isSetupComplete: Boolean) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = if (isSetupComplete) "home" else "setup"
    ) {
        composable("setup") {
            SetupScreen()
        }
        
        composable("home") {
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
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable(
            route = "song_detail/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) {
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
        ) {
            ProfileDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songId -> navController.navigate("song_detail/$songId") }
            )
        }

        composable(
            route = "category_songs/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) {
            CategorySongsScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { songId -> navController.navigate("song_detail/$songId") }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
