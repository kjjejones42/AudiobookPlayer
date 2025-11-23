package com.kjjejones42.audiobookplayer.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.kjjejones42.audiobookplayer.MainActivity
import com.kjjejones42.audiobookplayer.PlayerNavItem
import com.kjjejones42.audiobookplayer.database.AudiobookRepository
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import com.kjjejones42.audiobookplayer.database.models.AudioBookFile
import com.kjjejones42.audiobookplayer.database.models.AudioBookStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var audiobookRepository: AudiobookRepository

    private var mediaSession: MediaSession? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    // Create your player and media session in the onCreate lifecycle event
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .build()

        player.playWhenReady = false
        player.addListener(Listener(this))

        serviceScope.launch {
            player.listen {
                val currentItem = player.currentMediaItem
                val currentTrackIndex = player.currentMediaItemIndex
                val position = player.currentPosition

                if (currentItem != null) {
                    val audiobookId = currentItem.mediaMetadata.extras?.getString(INTENT_AUDIOBOOK)
                    if (audiobookId != null) {
                        audiobookRepository.updatePositions(audiobookId, currentTrackIndex, position.toInt())
                    }
                }
            }
        }

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            PlayerNavItem.URI.toUri(),
            this,
            MainActivity::class.java
        )

        val pendingIntent: PendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(deepLinkIntent)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            getPendingIntent(0, flags)!!
        }

//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, Intent(this, MainActivity::class.java),
//            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//        )

        mediaSession = MediaSession.Builder(this, player)
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
        val audioBook = audiobookRepository.getAudioBook(audioBookId)
        mediaSession?.let { session ->
            audioBook?.let { book ->
                val player = session.player
                val mediaItems = book.toMediaItems()
                if (mediaItems.isEmpty()) return
                val startTrackIndex = book.positionInTrackList
                val startPosition = if (startTrackIndex >= 0 && startTrackIndex < mediaItems.size) {
                    book.positionInTrack.toLong()
                } else {
                    0L
                }
                audiobookRepository.updatePositionInBook(audioBookId, startTrackIndex)
                audiobookRepository.updateStatus(audioBookId, AudioBookStatus.IN_PROGRESS)
                player.setMediaItems(mediaItems, startTrackIndex, startPosition)
                player.prepare()
                player.play()
            }
        }
    }

    fun playTrack(audioBookId: String, trackNo: Int?) {
        val audioBook = audiobookRepository.getAudioBook(audioBookId)
        mediaSession?.let { session ->
            audioBook?.let { book ->
                val player = session.player
                val currentAudioBookId = player.currentMediaItem?.mediaMetadata?.extras?.getString(INTENT_AUDIOBOOK)
                if (currentAudioBookId == audioBookId) {
                    val trackIndex = trackNo ?: player.currentMediaItemIndex
                    if (trackIndex in 0 until player.mediaItemCount) {
                        val position: Long = if (trackIndex == player.currentMediaItemIndex) {
                            book.positionInTrack.toLong()
                        } else {
                            0L
                        }
                        audiobookRepository.updatePositionInBook(audioBookId, trackIndex)
                        audiobookRepository.updateStatus(audioBookId, AudioBookStatus.IN_PROGRESS)
                        player.seekTo(trackIndex, position)
                        player.play()
                    }
                } else {
                    playBook(audioBookId)
                }
            }
        }
    }

    fun onMediaItemTransition(mediaItem: MediaItem) {
        val extras = mediaItem.mediaMetadata.extras ?: return
        val audiobookId = extras.getString(INTENT_AUDIOBOOK)
        val trackNo = extras.getInt(INTENT_INDEX)
        audiobookRepository.updatePositionInTrackList(audiobookId, trackNo)
    }

    fun handlePlaybackEnded() {
        val audioBookId = mediaSession?.player?.currentMediaItem?.mediaMetadata?.extras?.getString(INTENT_AUDIOBOOK)
        if (audioBookId != null) {
            audiobookRepository.updateStatus(audioBookId, AudioBookStatus.FINISHED)
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
        val PLAY_TRACK_COMMAND: SessionCommand = SessionCommand("com.example.app.PLAY_TRACK", Bundle.EMPTY)
        val PLAY_BOOK_COMMAND: SessionCommand = SessionCommand("com.example.app.PLAY_BOOK", Bundle.EMPTY)
        val FORWARD_THIRTY_COMMAND: SessionCommand = SessionCommand("com.example.app.FORWARD_THIRTY", Bundle.EMPTY)
        val BACK_THIRTY_COMMAND: SessionCommand = SessionCommand("com.example.app.BACK_THIRTY", Bundle.EMPTY)
    }
}

private fun AudioBookFile.toMediaItem(book: AudioBook, trackNo: Int): MediaItem? {
    val bookTitle = book.displayName
    val bookAuthor = book.author

    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(bookTitle)
        .setArtist(bookAuthor)
        .setSubtitle(fileName)
        .setDurationMs(duration)
        .setExtras(Bundle().apply {
            putInt(PlaybackService.INTENT_INDEX, trackNo)
            putString(PlaybackService.INTENT_AUDIOBOOK, book.displayName)
        })

    book.imagePath?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

    return MediaItem.Builder()
        .setMediaId("$bookTitle-$trackNo")
        .setUri(uri)
        .setMediaMetadata(metadataBuilder.build())
        .build()
}

private fun AudioBook.toMediaItems(): List<MediaItem> {
    return files
        ?.mapIndexed { index, trackFile -> trackFile.toMediaItem(this, index) }
        ?.filter { it != null }
        ?.map { it!! }
        ?.toList() ?: emptyList()
}

class Listener(private val service: PlaybackService) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem != null) {
            service.onMediaItemTransition(mediaItem)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            service.handlePlaybackEnded()
        }
    }

}

class Callback(private val service: PlaybackService) : MediaSession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()

        availableSessionCommands.add(PlaybackService.PLAY_TRACK_COMMAND)
        availableSessionCommands.add(PlaybackService.PLAY_BOOK_COMMAND)
        availableSessionCommands.add(PlaybackService.BACK_THIRTY_COMMAND)
        availableSessionCommands.add(PlaybackService.FORWARD_THIRTY_COMMAND)
        availableSessionCommands.add(Player.COMMAND_SEEK_TO_NEXT)
        availableSessionCommands.add(Player.COMMAND_SEEK_TO_PREVIOUS)

        return MediaSession.ConnectionResult.accept(
            availableSessionCommands.build(),
            connectionResult.availablePlayerCommands
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {

        when (customCommand.customAction) {

            PlaybackService.PLAY_TRACK_COMMAND.customAction -> {
                val audioBookId = args.getString(PlaybackService.INTENT_AUDIOBOOK)
                if (audioBookId != null) {
                    val trackId = args.getInt(PlaybackService.INTENT_INDEX)
                    service.playTrack(audioBookId, trackId)
                }
            }

            PlaybackService.PLAY_BOOK_COMMAND.customAction -> {
                val audioBookId = args.getString(PlaybackService.INTENT_AUDIOBOOK)
                if (audioBookId != null) {
                    service.playBook(audioBookId)
                }
            }

            PlaybackService.FORWARD_THIRTY_COMMAND.customAction -> {
                service.onForwardThirtyCommand()
            }

            PlaybackService.BACK_THIRTY_COMMAND.customAction -> {
                service.onBackThirtyCommand()
            }

            else -> return super.onCustomCommand(session, controller, customCommand, args)
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}