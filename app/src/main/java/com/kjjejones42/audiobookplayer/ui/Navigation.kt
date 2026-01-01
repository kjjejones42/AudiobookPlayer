package com.kjjejones42.audiobookplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import kotlinx.serialization.Serializable

@Serializable
object DisplayListNavItem

@Serializable
object PlayerNavItem {
    const val URI = "https://com.kjjejones42.audiobookplayer/player/"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mediaController: MediaController?,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = DisplayListNavItem,
    ) {
        composable<DisplayListNavItem> {
            DisplayListComposable(
                mediaController = mediaController,
                onBookClick = { navController.navigate(PlayerNavItem) },
            )
        }
        composable<PlayerNavItem>(
            deepLinks = listOf(navDeepLink { uriPattern = PlayerNavItem.URI }),
        ) {
            PlayerComposable(
                onBack = { navController.navigate(DisplayListNavItem) },
                mediaController = mediaController,
            )
        }
    }
}
