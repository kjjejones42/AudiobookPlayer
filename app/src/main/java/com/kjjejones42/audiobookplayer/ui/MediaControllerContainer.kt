package com.kjjejones42.audiobookplayer.ui

import android.content.ComponentName
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kjjejones42.audiobookplayer.services.PlaybackService

@Composable
fun MediaControllerContainer(content: @Composable (MediaController?) -> Unit) {
    val context = LocalContext.current

    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java),
            )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
            } catch (e: Exception) {
                Log.e("MediaControllerContainer", "Failed to connect to MediaSession", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }
    content(mediaController)
}
