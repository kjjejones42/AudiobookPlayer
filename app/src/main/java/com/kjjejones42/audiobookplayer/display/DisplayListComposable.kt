package com.kjjejones42.audiobookplayer.display

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.compose.state.PlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kjjejones42.audiobookplayer.MainActivity
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import com.kjjejones42.audiobookplayer.database.models.AudioBookStatus
import com.kjjejones42.audiobookplayer.player.PlaybackService
import com.kjjejones42.audiobookplayer.ui.AudiobookPlayerTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayListComposable(
    viewModel: DisplayListViewModel = hiltViewModel(),
    onBookClick: () -> Unit = {},
    mediaController: MediaController? = null,
) {
    val context = LocalContext.current

    val items: List<ListItem> by viewModel.listItems.collectAsStateWithLifecycle()
    val workUUID by viewModel.workUuid.collectAsStateWithLifecycle()
    val workInfo by viewModel.getWorkStateFlow(LocalContext.current).collectAsState(null)
    val categorySelectBook by viewModel.categorySelectBook.collectAsStateWithLifecycle()

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
            val request = OneTimeWorkRequestBuilder<FileScannerWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
            val workUuid = request.id
            viewModel.workUuid.value = workUuid
            viewModel.workNeedsStarting.value = false
        }
    }

    // Request permissions on first launch
    LaunchedEffect(viewModel) {
        if (workUUID == null) {
            permissionLauncher.launch(MainActivity.PERMISSIONS)
        }
    }

    val playState: PlayPauseButtonState? = if (mediaController != null) {
        rememberPlayPauseButtonState(mediaController)
    } else null

    val isPlaying = !(playState?.showPlay ?: true)

    DisplayListScreenBase(
        items = items,
        onBookClick = { startBook(it, mediaController, onBookClick) },
        isPlaying = isPlaying,
        categorySelectBook = categorySelectBook,
        setCategorySelectBook = { viewModel.categorySelectBook.value = it },
        updateBookStatus = viewModel::updateBookStatus,
        onFABClick = {
            if (isPlaying) {
                mediaController?.pause()
                return@DisplayListScreenBase
            } else {
                val book = viewModel.getMostRecentBook() ?: return@DisplayListScreenBase
                startBook(book, mediaController, onBookClick)
            }
        }
    )
}

fun startBook(book: AudioBook, mediaController: MediaController?, onBookClick: () -> Unit) {
    mediaController?.let {
        val bundle = Bundle().apply{ putString(PlaybackService.INTENT_AUDIOBOOK, book.displayName) }
        mediaController.sendCustomCommand(PlaybackService.PLAY_BOOK_COMMAND, bundle)
    }
    onBookClick()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayListScreenBase(
    items: List<ListItem>,
    onBookClick: (AudioBook) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    isPlaying: Boolean = false,
    onFABClick: () -> Unit = {},
    categorySelectBook: AudioBook? = null,
    setCategorySelectBook: (AudioBook?) -> Unit = {},
    updateBookStatus: (AudioBook, AudioBookStatus) -> Unit = { _, _ -> }
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
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play_button)
                )
            }
        }
    ) { paddingValues ->

        categorySelectBook?.let { categorySelectBook ->
            MinimalDialog(
                book = categorySelectBook,
                onDismissRequest = { setCategorySelectBook(null) },
                updateBookStatus = { book, status ->
                    setCategorySelectBook(null)
                    updateBookStatus(book, status)
                }
            )
        }

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
                                onClick = onBookClick,
                                onLongClick = { setCategorySelectBook(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalDialog(
    book: AudioBook,
    onDismissRequest: () -> Unit,
    updateBookStatus: (AudioBook, AudioBookStatus) -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),

        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Add some padding inside the column
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose this book's status.",
                    textAlign = TextAlign.Center,
                )
                AudioBookStatus.entries.forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = { updateBookStatus(book, status) }
                            ),
                        verticalAlignment = Alignment.CenterVertically,

                    ){
                        RadioButton(
                            selected = status.value == book.status,
                            onClick = { updateBookStatus(book, status) }
                        )
                        Text(text = status.displayName)
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
                return@filter item.book.toString().contains(searchQuery, ignoreCase = true)
            }
            return@filter false
        }.sortedBy { it.id }
        .toList()
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
