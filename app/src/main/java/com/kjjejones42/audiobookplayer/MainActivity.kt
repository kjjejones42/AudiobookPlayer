package com.kjjejones42.audiobookplayer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.kjjejones42.audiobookplayer.display.MediaControllerContainer
import com.kjjejones42.audiobookplayer.ui.AudiobookPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MediaControllerContainer { mediaController ->
                AudiobookPlayerTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(
                            navController = rememberNavController(),
                            mediaController = mediaController
                        )
                    }
                }
            }
        }
    }

    companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        const val INTENT_PLAY_FILE: String = "com.kjjejones42.audiobookplayer.PLAY"
        const val INTENT_START_PLAYBACK: String = "com.kjjejones42.audiobookplayer.start"
    }
}