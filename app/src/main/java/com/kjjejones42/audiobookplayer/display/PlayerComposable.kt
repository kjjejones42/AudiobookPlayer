package com.kjjejones42.audiobookplayer.display

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.ui.AudiobookPlayerTheme
import kotlinx.coroutines.launch

@Composable
fun PlayerComposable(
    audioBookId: String,
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    mediaController: MediaController? = null,
) {
    viewModel.setAudioBook(audioBookId)
    val audiobook: AudioBook? by viewModel.audioBook.collectAsStateWithLifecycle()
    audiobook?.let {
        PlayerComposableBase(
            audioBook = audiobook!!,
            onBack = onBack,
            mediaController = mediaController
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerComposableBase(
    audioBook: AudioBook,
    imageRequestData: Any? = audioBook.imagePath,
    onBack: () -> Unit = {},
    mediaController: MediaController? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap: BitmapImage? by remember { mutableStateOf(null) }
    var theme: ColorScheme? by remember { mutableStateOf(null) }

    val currentTheme = MaterialTheme.colorScheme
    val isDarkMode = isSystemInDarkTheme()
    LaunchedEffect(bitmap) {
        if (bitmap != null) {
            scope.launch {
                theme = extractColorPalette(bitmap, isDarkMode, currentTheme)
            }
        }
    }

    val canHaveTheme = imageRequestData != null
    LaunchedEffect(imageRequestData) {
        bitmap = null
        theme = null
        if (canHaveTheme) {
            scope.launch {
                val request = ImageRequest.Builder(context).data(imageRequestData).build()
                val image = context.imageLoader.enqueue(request).job.await().image
                if (image is BitmapImage) { bitmap = image }
            }
        }
    }

    val themeLoaded = bitmap != null && theme != null
//    if (canHaveTheme && !themeLoaded) return


    if (mediaController == null) return
    val mediaController: MediaController = mediaController

    val playPauseState = rememberPlayPauseButtonState(mediaController)
    val positionState = rememberProgressStateWithTickInterval(mediaController)

    MaterialTheme(theme ?: MaterialTheme.colorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(audioBook.displayName) },
                    navigationIcon = {
                        IconButton(onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = imageRequestData,
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
                if (positionState.durationMs != C.TIME_UNSET) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = positionState.currentPositionMs.toString())
                        Slider(
                            value = positionState.currentPositionMs.toFloat(),
                            onValueChange = { mediaController.seekTo(it.toLong()) },
                            valueRange = 0.0f..positionState.durationMs.toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = positionState.durationMs.toString())
                    }
                }
                Row(){
                    playPauseState.showPlay
                    FilledIconButton({ playPauseState.onClick() }) {
                        Icon(
                            imageVector = if (playPauseState.showPlay) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }

                }
            }
        }
    }
}

fun extractColorPalette(bitmap: BitmapImage?, isDarkMode: Boolean, currentTheme: ColorScheme): ColorScheme {
    bitmap ?: return currentTheme
    val palette = Palette.from(bitmap.bitmap.copy(Bitmap.Config.ARGB_8888, true)).generate()
    val swatch: Palette.Swatch? = if (isDarkMode) {
        palette.darkVibrantSwatch ?: palette.darkMutedSwatch
    } else {
        palette.lightVibrantSwatch ?: palette.lightMutedSwatch
    }
    swatch ?: return currentTheme
    val seedColor = Color(swatch.rgb)
    return currentTheme.copy(
        primary = seedColor,
        surface = seedColor,
        primaryContainer = seedColor,
    )
}

@Composable
@Preview
fun PlayerComposablePreview() {
    val book = AudioBook("Title", "", "", emptyList(), "Author")
    AudiobookPlayerTheme(inDarkTheme = false) {
        PlayerComposableBase(book, imageRequestData = R.drawable.test)
    }
}