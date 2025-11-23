package com.kjjejones42.audiobookplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kjjejones42.audiobookplayer.display.DisplayListScreen
import com.kjjejones42.audiobookplayer.display.PlayerComposable
import kotlinx.serialization.Serializable


@Serializable
object DisplayListNavItem

@Serializable
data class PlayerNavItem(val audiobookId: String)

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mediaController: MediaController?
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = DisplayListNavItem
    ) {
        composable<DisplayListNavItem> {
            DisplayListScreen(
                mediaController = mediaController,
                onBookClick = { book -> navController.navigate(PlayerNavItem(book.displayName)) }
            )
        }
        composable<PlayerNavItem> { backStackEntry ->
            val book: PlayerNavItem = backStackEntry.toRoute()
            PlayerComposable(
                audioBookId = book.audiobookId,
                onBack = { navController.navigate(DisplayListNavItem) },
                mediaController = mediaController
            )
        }

    }
}