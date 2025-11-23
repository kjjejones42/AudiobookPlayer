package com.kjjejones42.audiobookplayer.display

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kjjejones42.PlaybackService
import com.kjjejones42.audiobookplayer.AppNavHost
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.AudioBookStatus
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase
import com.kjjejones42.audiobookplayer.player.MediaPlaybackService
import com.kjjejones42.audiobookplayer.player.PlayActivity
import com.kjjejones42.audiobookplayer.ui.AudiobookPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayListScreen(
    viewModel: DisplayListViewModel = hiltViewModel(),
    onBookClick: (AudioBook) -> Unit = {},
    mediaController: MediaController? = null,
) {
    val context = LocalContext.current

    val items: List<ListItem> by viewModel.listItems.collectAsStateWithLifecycle()
    val workUUID by viewModel.workUuid.collectAsStateWithLifecycle()
    val workInfo by viewModel.getWorkStateFlow(LocalContext.current).collectAsState(null)

    val workNeedsStarting by viewModel.workNeedsStarting.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all{it}) {
            viewModel.workNeedsStarting.value = true
        } else {
            Log.e("DisplayListScreen", "Not all permissions granted")
            Toast.makeText(context, "Not all permissions granted", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(workNeedsStarting) {
        if (workNeedsStarting && viewModel.workUuid.value == null) {
            val workUuid = startFileScanning(context)
            viewModel.workUuid.value = workUuid
            viewModel.workNeedsStarting.value = false
        }
    }

    // Request permissions on first launch
    LaunchedEffect(viewModel) {
        if (workUUID == null) {
            permissionLauncher.launch(DisplayListActivity.PERMISSIONS)
        }
    }

    fun onBookClickIN(book: AudioBook) {
        mediaController?.let {
            val mediaItem = book.toMediaItem()
            if (mediaController!!.currentMediaItem?.mediaId != mediaItem.mediaId) {
                it.setMediaItem(mediaItem)
                it.prepare()
            }
            it.seekTo(book.positionInTrack.toLong())
            it.play()
        }
        onBookClick(book)
    }


    DisplayListScreenBase(
        items = items,
        workInfo = workInfo,
        onBookClick = { onBookClickIN(it) },
        isPlaying = mediaController?.isPlaying ?: false
    )
}

private fun AudioBook.toMediaItem(): MediaItem {
    val metadataBuilder = MediaMetadata.Builder()
        .setArtworkUri(android.net.Uri.parse(imagePath))
        .setTitle(displayName)
        .setArtist(author)
    val builder = MediaItem.Builder()
        .setMediaId(displayName)
    files?.get(positionInTrackList)?.let {
        builder.setUri(it.uri)
        metadataBuilder.setDurationMs(it.duration)
    }
    builder.setMediaMetadata(metadataBuilder.build())
    return builder.build()

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayListScreenBase(
    items: List<ListItem>,
    workInfo: WorkInfo? = null,
    onBookClick: (AudioBook) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    isPlaying: Boolean = false,
    onFABClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredItems = if (isSearchActive) filterItems(items, searchQuery) else items

    // Handle back press for search
    BackHandler(enabled = isSearchActive) {
        searchQuery = ""
        isSearchActive = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audiobook Player") },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onFABClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.AddCircle else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play_button)
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            when {
//                workInfo != null && workInfo.state == WorkInfo.State.RUNNING -> {
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        CircularProgressIndicator()
//                    }
//                }
                filteredItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_data_available),
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            ListItemComposable(
                                item = item,
                                onClick = onBookClick
                            )
                        }
                    }
                }
            }
        }
    }
}

fun filterItems(
    items: List<ListItem>,
    searchQuery: String
): List<ListItem> {
    return items.filter { item ->
            if (item is ListItem.AudioBookContainer) {
                return@filter item.book.displayName.contains(searchQuery, ignoreCase = true)
                        || (item.book.author ?: "").contains(searchQuery, ignoreCase = true)
            }
            return@filter false
        }.sortedBy { it.id }
        .toList()
}

private fun startFileScanning(context: Context): UUID {
    val request = OneTimeWorkRequestBuilder<FileScannerWorker>().build()
    WorkManager.getInstance(context).enqueue(request)
    return request.id
}

private fun resumeMostRecentBook(context: Context) {
    AudiobookDatabase.getInstance(context)
        .audiobookDao()
        .mostRecentBook
        ?.takeIf { it.lastSavedTimestamp > 0 }
        ?.let {
            val intent = Intent(context, MediaPlaybackService::class.java)
            intent.putExtra(PlayActivity.INTENT_AUDIOBOOK, it.displayName)
            intent.putExtra(PlayActivity.INTENT_INDEX, it.positionInTrackList)
            context.startService(intent)
        }
}

@Composable
private fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

private fun startBook(context: Context, book: AudioBook) {
    val intent = Intent(context, MediaPlaybackService::class.java)
    intent.putExtra(PlayActivity.INTENT_AUDIOBOOK, book.displayName)
    intent.putExtra(PlayActivity.INTENT_INDEX, book.positionInTrackList)
    context.startService(intent)
}

@AndroidEntryPoint
class DisplayListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val intent = Intent(this, PlaybackService::class.java)
        ContextCompat.startForegroundService(this, intent)

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
            Manifest.permission.READ_MEDIA_AUDIO,
//            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        const val INTENT_PLAY_FILE: String = "com.kjjejones42.audiobookplayer.PLAY"
        const val INTENT_START_PLAYBACK: String = "com.kjjejones42.audiobookplayer.start"
    }
}

@Composable
fun MediaControllerContainer(
    content: @Composable (MediaController?) -> Unit
) {
    val context = LocalContext.current

    var isConnecting by remember { mutableStateOf(true) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                isConnecting = false
                onDispose {
                    MediaController.releaseFuture(controllerFuture)
                }
            } catch (e: Exception) {
                isConnecting = false
                MediaController.releaseFuture(controllerFuture)
                onDispose {} // Still need to define an onDispose
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            if (mediaController == null && isConnecting) {
                MediaController.releaseFuture(controllerFuture)
            }
        }
    }
    content(mediaController)
}

@Composable
@Preview
fun Preview () {
    val books: List<AudioBook> = listOf(
        AudioBook("TITLE TITLE TITLE A", "", "", emptyList(), "Author McAuthor").apply { status = AudioBookStatus.IN_PROGRESS.value },
        AudioBook("TITLE TITLE TITLE B", "", "", emptyList(), "Author McAuthor"),
        AudioBook("TITLE TITLE TITLE C", "", "", emptyList(), "Author McAuthor")
    )
    val workInfo = WorkInfo(id = UUID.randomUUID(), state = WorkInfo.State.RUNNING, tags = emptySet())
    val items = DisplayListViewModel.getItemsFromBooks(books)
    AudiobookPlayerTheme(inDarkTheme = false) {
        DisplayListScreenBase(items)
    }
}
