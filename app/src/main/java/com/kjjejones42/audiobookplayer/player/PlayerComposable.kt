package com.kjjejones42.audiobookplayer.player

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import com.kjjejones42.audiobookplayer.database.models.AudioBookFile
import com.kjjejones42.audiobookplayer.ui.AudiobookPlayerTheme
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit


private fun msToMMSS(ms: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    if (hours > 0) {
        return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Composable
fun PlayerComposable(
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    mediaController: MediaController? = null,
) {
    if (mediaController == null) return
    val mediaController = mediaController!!
    val playPauseState = rememberPlayPauseButtonState(mediaController)
    val positionState = rememberProgressStateWithTickInterval(mediaController)

    val extras = mediaController.currentMediaItem?.mediaMetadata?.extras
    extras?.let {
        viewModel.setTrackNo(it.getInt(PlaybackService.INTENT_INDEX))
        viewModel.setAudioBook(it.getString(PlaybackService.INTENT_AUDIOBOOK))
    }

    val trackNo by viewModel.trackNo.collectAsStateWithLifecycle()
    val audiobook: AudioBook? by viewModel.audioBook.collectAsStateWithLifecycle()

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) { onBack() }
            }
        }
        mediaController.addListener(listener)
        onDispose {
            mediaController.removeListener(listener)
        }
    }

    audiobook?.let { audiobook ->
        PlayerComposableBase(
            audiobook.displayName,
            imageRequestData =  audiobook.imagePath ?: audiobook.files?.get(0),
            onBack = onBack,
            currentPositionMs = positionState.currentPositionMs,
            durationMs = positionState.durationMs,
            seekTo = { mediaController.seekTo(it) },
            showPlay = playPauseState.showPlay,
            onPlayPauseClick = { playPauseState.onClick() },
            files = audiobook.files ?: emptyList(),
            currentTrackIndex = trackNo ?: audiobook.positionInTrackList,
            playTrack = { index -> playTrack(mediaController, audiobook, index) },
            onNext = { mediaController.seekToNext() },
            onPrev = { mediaController.seekToPrevious() },
            onBackThirty = { mediaController.sendCustomCommand(PlaybackService.BACK_THIRTY_COMMAND, Bundle.EMPTY) },
            onForwardThirty = { mediaController.sendCustomCommand(PlaybackService.FORWARD_THIRTY_COMMAND, Bundle.EMPTY) }
        )
    }
}

fun playTrack(controller: MediaController, book: AudioBook, index: Int) {
    val bundle = Bundle().apply {
        putString(PlaybackService.INTENT_AUDIOBOOK, book.displayName)
        putInt(PlaybackService.INTENT_INDEX, index)
    }
    controller.sendCustomCommand(PlaybackService.PLAY_TRACK_COMMAND, bundle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerComposableBase(
    displayName: String,
    imageRequestData: Any?,
    onBack: () -> Unit = {},
    currentPositionMs: Long = 0,
    durationMs: Long = C.TIME_UNSET,
    seekTo: (Long) -> Unit = {},
    showPlay: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    files: List<AudioBookFile> = emptyList(),
    currentTrackIndex: Int = 0,
    playTrack: (Int) -> Unit = {},
    onNext: () -> Unit = {},
    onPrev: () -> Unit = {},
    onForwardThirty: () -> Unit = {},
    onBackThirty: () -> Unit = {}
) {
    val durationMs = durationMs.coerceAtLeast(0)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap: BitmapImage? by remember { mutableStateOf(null) }
    var theme: ColorScheme? by remember { mutableStateOf(null) }
    var expanded by remember { mutableStateOf(false) }

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

//    val themeLoaded = theme != null
//    if (canHaveTheme && !themeLoaded && !LocalInspectionMode.current) return

    MaterialTheme(theme ?: MaterialTheme.colorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(displayName) },
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
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageRequestData,
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .blur(30.dp)
                    )
                    AsyncImage(
                        model = imageRequestData,
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                ){
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = files[currentTrackIndex].fileName,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.MoreVert, contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        files.forEachIndexed { index, file ->
                            DropdownMenuItem(
                                text = { Text(file.fileName) },
                                onClick = {
                                    expanded = false
                                    playTrack(index)
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = msToMMSS(currentPositionMs),
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = currentPositionMs.toFloat(),
                        onValueChange = { seekTo(it.toLong()) },
                        valueRange = 0.0f..durationMs.toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = msToMMSS(durationMs),
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerButton(
                        image = Icons.Filled.SkipPrevious,
                        onClick = onPrev
                    )
                    PlayerButton(
                        image = ImageVector.vectorResource(id = R.drawable.ic_replay_30),
                        onClick = onBackThirty
                    )
                    FilledIconButton(
                        modifier = Modifier.size(64.dp),
                        onClick = { onPlayPauseClick() },
                    ) {
                        Icon(imageVector = if (showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null
                        )
                    }
                    PlayerButton(
                        image = ImageVector.vectorResource(id = R.drawable.ic_forward_30),
                        onClick = onForwardThirty
                    )
                    PlayerButton(
                        image = Icons.Filled.SkipNext,
                        onClick = onNext
                    )

                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PlayerButton(
    image: ImageVector,
    onClick: () -> Unit = {},
) {
    FilledIconButton(onClick = { onClick() }) {
        Icon(imageVector = image, contentDescription = null)
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
    val files: List<AudioBookFile> = listOf(
        AudioBookFile(Uri.parse(""), "A", "D", 1),
        AudioBookFile(Uri.parse(""), "B", "E", 1),
        AudioBookFile(Uri.parse(""), "C", "F", 1)
    )
    AudiobookPlayerTheme(inDarkTheme = false) {
        PlayerComposableBase(
            book.displayName,
             null,
            durationMs = 75 * 1000,
            currentPositionMs = 3500,
            files = files
        )
    }
}