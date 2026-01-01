package com.kjjejones42.audiobookplayer.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kjjejones42.audiobookplayer.MainActivity
import com.kjjejones42.audiobookplayer.database.AudiobookRepository
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import com.kjjejones42.audiobookplayer.database.models.AudioBookFile
import com.kjjejones42.audiobookplayer.ui.PlayerNavItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject
    lateinit var audiobookRepository: AudiobookRepository

    private var mediaSession: MediaSession? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build()

        val player =
            ExoPlayer
                .Builder(this)
                .setHandleAudioBecomingNoisy(true)
                .setAudioAttributes(audioAttributes, true)
                .build()

        player.playWhenReady = false
        player.addListener(Listener(this))

        serviceScope.launch {
            player.listenTo(Player.EVENT_IS_PLAYING_CHANGED) {
                val currentItem = player.currentMediaItem
                val positionInTrackList = player.currentMediaItemIndex
                val positionInTrack = player.currentPosition

                currentItem?.let { currentItem ->
                    val audiobookId = currentItem.mediaMetadata.extras?.getString(INTENT_AUDIOBOOK)
                    audiobookId?.let { audiobookId ->
                        launch {
                            audiobookRepository.updatePositions(
                                audiobookId,
                                positionInTrackList,
                                positionInTrack.toInt(),
                            )
                        }
                    }
                }
            }
        }

        val deepLinkIntent =
            Intent(
                Intent.ACTION_VIEW,
                PlayerNavItem.URI.toUri(),
                this,
                MainActivity::class.java,
            )

        val pendingIntent: PendingIntent =
            TaskStackBuilder.create(this).run {
                addNextIntentWithParentStack(deepLinkIntent)
                val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                getPendingIntent(0, flags)!!
            }

        mediaSession =
            MediaSession
                .Builder(this, player)
                .setSessionActivity(pendingIntent)
                .setCallback(Callback(this))
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        serviceJob.cancel()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    fun playBook(audioBookId: String) {
        val audioBook = runBlocking { audiobookRepository.getAudioBook(audioBookId) }
        mediaSession?.let { session ->
            audioBook?.let { book ->
                val mediaItems = book.toMediaItems()
                if (mediaItems.isEmpty()) return
                val startTrackIndex = book.positionInTrackList
                val startPosition =
                    if (startTrackIndex >= 0 && startTrackIndex < mediaItems.size) {
                        book.positionInTrack.toLong()
                    } else {
                        0L
                    }

                val player = session.player
                player.setMediaItems(mediaItems, startTrackIndex, startPosition)
                player.prepare()
                player.play()
                serviceScope.launch { audiobookRepository.updatePositionInBook(audioBookId, startTrackIndex) }
            }
        }
    }

    fun onMediaItemTransition(mediaItem: MediaItem) {
        val extras = mediaItem.mediaMetadata.extras ?: return
        val audiobookId = extras.getString(INTENT_AUDIOBOOK)
        val trackNo = extras.getInt(INTENT_INDEX)
        serviceScope.launch { audiobookRepository.updatePositionInTrackList(audiobookId, trackNo) }
    }

    fun handlePlaybackEnded() {
        val audioBookId =
            mediaSession
                ?.player
                ?.currentMediaItem
                ?.mediaMetadata
                ?.extras
                ?.getString(INTENT_AUDIOBOOK)
        audioBookId?.let { audioBookId ->
            serviceScope.launch { audiobookRepository.updateStatus(audioBookId, AudioBook.Status.FINISHED) }
        }
    }

    fun onForwardThirtyCommand() {
        val player = mediaSession?.player ?: return
        val position = player.currentPosition
        val newPosition = (position + 30000).coerceAtMost(player.duration)
        player.seekTo(newPosition)
    }

    fun onBackThirtyCommand() {
        val player = mediaSession?.player ?: return
        val position = player.currentPosition
        val newPosition = (position - 30000).coerceAtLeast(0)
        player.seekTo(newPosition)
    }

    companion object {
        const val INTENT_AUDIOBOOK: String = "AUDIOBOOK"
        const val INTENT_INDEX: String = "INDEX"
        val PLAY_BOOK_COMMAND: SessionCommand = SessionCommand("com.example.app.PLAY_BOOK", Bundle.EMPTY)
        val FORWARD_THIRTY_COMMAND: SessionCommand = SessionCommand("com.example.app.FORWARD_THIRTY", Bundle.EMPTY)
        val BACK_THIRTY_COMMAND: SessionCommand = SessionCommand("com.example.app.BACK_THIRTY", Bundle.EMPTY)
    }
}

private fun AudioBookFile.toMediaItem(
    book: AudioBook,
    trackNo: Int,
): MediaItem {
    val bookTitle = book.displayName
    val bookAuthor = book.author

    val metadataBuilder =
        MediaMetadata
            .Builder()
            .setTitle(bookTitle)
            .setArtist(bookAuthor)
            .setSubtitle(fileName)
            .setDurationMs(duration)
            .setExtras(
                Bundle().apply {
                    putInt(PlaybackService.INTENT_INDEX, trackNo)
                    putString(PlaybackService.INTENT_AUDIOBOOK, book.displayName)
                },
            )

    book.imagePath?.let { metadataBuilder.setArtworkUri(it.toUri()) }

    return MediaItem
        .Builder()
        .setMediaId("$bookTitle-$trackNo")
        .setUri(uri)
        .setMediaMetadata(metadataBuilder.build())
        .build()
}

private fun AudioBook.toMediaItems(): List<MediaItem> =
    files
        ?.mapIndexed { index, trackFile -> trackFile.toMediaItem(this, index) }
        ?.toList() ?: emptyList()

private class Listener(
    private val service: PlaybackService,
) : Player.Listener {
    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        mediaItem?.let { mediaItem -> service.onMediaItemTransition(mediaItem) }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            service.handlePlaybackEnded()
        }
    }
}

private class Callback(
    private val service: PlaybackService,
) : MediaSession.Callback {
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val availableCommands = connectionResult.availableSessionCommands.buildUpon()

        availableCommands.add(PlaybackService.PLAY_BOOK_COMMAND)
        availableCommands.add(PlaybackService.BACK_THIRTY_COMMAND)
        availableCommands.add(PlaybackService.FORWARD_THIRTY_COMMAND)

        return MediaSession.ConnectionResult.accept(
            availableCommands.build(),
            connectionResult.availablePlayerCommands,
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            PlaybackService.PLAY_BOOK_COMMAND.customAction -> {
                val audioBookId = args.getString(PlaybackService.INTENT_AUDIOBOOK)
                audioBookId?.let { audioBookId -> service.playBook(audioBookId) }
            }

            PlaybackService.FORWARD_THIRTY_COMMAND.customAction -> {
                service.onForwardThirtyCommand()
            }

            PlaybackService.BACK_THIRTY_COMMAND.customAction -> {
                service.onBackThirtyCommand()
            }

            else -> {
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}
