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
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
