package com.kjjejones42.audiobookplayer.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.MediaItem
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.database.AudiobookDao
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance
import com.kjjejones42.audiobookplayer.display.DisplayListActivity
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val mediaPlayer = MediaPlayer()

    private var notificationManager: PlayerNotificationManager? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private var metadataBuilder: MediaMetadataCompat.Builder? = null
    private lateinit var mediaSession: MediaSessionCompat
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioAttributes: AudioAttributes? = null
        get() {
            if (field == null) {
                field = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            }
            return field
        }
    private var bookId: String? = null
    private var updateTask: Timer? = null
    private var isMediaPlayerPrepared = false
    private var intentId = 0
    private val onAudioFocusChangeListener: OnAudioFocusChangeListener =
        object : OnAudioFocusChangeListener {
            var lastKnownAudioFocusState: Int = 0
            var wasPlayingWhenTransientLoss: Boolean = false

            override fun onAudioFocusChange(focusChange: Int) {
                val controls = mediaSession.controller.transportControls
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> when (lastKnownAudioFocusState) {
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (wasPlayingWhenTransientLoss) {
                            controls.play()
                        }

                        else -> controls.play()
                    }

                    AudioManager.AUDIOFOCUS_LOSS -> controls.pause()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        wasPlayingWhenTransientLoss = mediaPlayer.isPlaying
                        controls.pause()
                    }
                }
                lastKnownAudioFocusState = focusChange
            }
        }

    private lateinit var dao: AudiobookDao

    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var hasBeenInterrupted = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                mediaSession.controller.transportControls.pause()
                hasBeenInterrupted = true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intentId = startId
        if (intent != null) {
            try {
                if (updateTask != null) {
                    updateTask!!.cancel()
                }

                val positionInTrackList = intent.getIntExtra(PlayActivity.INTENT_INDEX, 0)
                bookId = intent.getSerializableExtra(
                    PlayActivity.INTENT_AUDIOBOOK,
                    String::class.java
                )
                checkNotNull(bookId)

                val resumeIntent = Intent(this, PlayActivity::class.java)
                resumeIntent.putExtra(DisplayListActivity.INTENT_PLAY_FILE, bookId)
                mediaSession.setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        2,
                        resumeIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                var positionInTrack = dao.getPositionInTrack(bookId)
                if (dao.getStatus(bookId) == AudioBook.STATUS_FINISHED) {
                    updateStatus(AudioBook.STATUS_IN_PROGRESS)
                } else {
                    if (positionInTrackList != dao.getPositionInTrackList(bookId)) {
                        positionInTrack = 1
                        dao.updatePositionInTrack(bookId, 1, System.currentTimeMillis())
                    }
                }
                setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        positionInTrack.toLong(),
                        1f
                    ).build()
                )
                playTrack(positionInTrackList)
            } catch (e: Exception) {
                onError()
                throw e
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private val onCompletionListener: OnCompletionListener = OnCompletionListener {
        val state = mediaSession.controller.playbackState.state
        if (state != PlaybackStateCompat.STATE_SKIPPING_TO_NEXT && state != PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS) {
            mediaSession.controller.transportControls.pause()
            if (updateTask != null) {
                updateTask!!.cancel()
            }
            dao.updatePositionInTrack(bookId, 1, System.currentTimeMillis())
            dao.getPositionInTrackList(bookId).plus(1).let { playTrack(it) }
        }
    }

    private val request: AudioFocusRequest?
        get() {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes!!)
                    .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                    .build()
            }
            return audioFocusRequest
        }

    private fun getIsMediaPlayerPrepared(): Boolean {
        return (mediaSession.controller.playbackState != null) && isMediaPlayerPrepared
    }

    private val isMediaPlayerPlaying: Boolean
        get() {
            return try {
                mediaPlayer.isPlaying
            } catch (e: IllegalStateException) {
                false
            }
        }

    private val isPlaying: Boolean
        get() {
            if (mediaSession.controller.playbackState == null) {
                return false
            }
            return mediaSession.controller.playbackState
                .state == PlaybackStateCompat.STATE_PLAYING
        }

    private fun updateStatus(status: Int) {
        val book = dao.findByName(bookId)
        book!!.setStatus(status)
        dao.update(book)
    }

    private fun setPlaybackState(state: PlaybackStateCompat) {
        synchronized(mediaSession) {
            val positionInTrack = state.position.toInt()
            if (positionInTrack != 0) {
                dao.updatePositionInTrack(
                    bookId,
                    state.position.toInt(),
                    System.currentTimeMillis()
                )
            }
            mediaSession.setPlaybackState(state)
        }
    }

    private fun playTrack(positionInTrackList: Int) {
        if (positionInTrackList < 0) {
            return
        }
        val files = dao.findByName(
            bookId
        )!!.files
        if (positionInTrackList >= files!!.size) {
            updateStatus(AudioBook.STATUS_FINISHED)
            mediaSession.controller.transportControls.sendCustomAction(EVENT_REACHED_END, null)
            return
        }
        dao.updatePositionInTrackList(bookId, positionInTrackList)
        val mediaItem = files[positionInTrackList]
        mediaPlayer.reset()
        isMediaPlayerPrepared = false
        if (updateTask != null) {
            updateTask!!.cancel()
        }
        mediaPlayer.setAudioAttributes(audioAttributes)
        try {
            mediaPlayer.setOnCompletionListener(onCompletionListener)
            mediaPlayer.setDataSource(this, mediaItem.getUri())
            mediaPlayer.prepare()
            isMediaPlayerPrepared = true
            mediaSession.setMetadata(trackToMetaData(mediaItem))
            mediaSession.controller.transportControls.seekTo(
                dao.getPositionInTrack(bookId).toLong()
            )
            mediaSession.controller.transportControls.play()
        } catch (e: IOException) {
            onError()
            throw RuntimeException(e)
        }
    }

    private fun onError() {
        stateBuilder.setState(PlaybackStateCompat.STATE_ERROR, 0, 1f)
        setPlaybackState(stateBuilder.build())
        stopSelf(intentId)
    }

    private fun trackToMetaData(item: MediaItem): MediaMetadataCompat? {
        try {
            MediaMetadataRetriever().use { mmr ->
                mmr.setDataSource(this, item.getUri())
                val durationStr =
                    checkNotNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
                val duration = durationStr.toLong()
                val book = dao.findByName(bookId)
                metadataBuilder
                    ?.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.toString())
                    ?.putString(MediaMetadataCompat.METADATA_KEY_TITLE, book!!.displayName)
                    ?.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, book.getAlbumArt(this))
                    ?.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                    ?.putLong(
                        MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                        book.files!!.indexOf(item).toLong()
                    )
                    ?.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, book.uniqueId)
                return metadataBuilder!!.build()
            }
        } catch (ignored: IOException) {
            return null
        }
    }

    private fun initialiseTimer() {
        if (updateTask != null) {
            updateTask!!.cancel()
        }
        updateTask = Timer()
        updateTask!!.schedule(object : TimerTask() {
            override fun run() {
                try {
                    if (isMediaPlayerPlaying) {
                        val position = mediaPlayer.currentPosition
                        if (position != 0) {
                            val state = mediaSession.controller.playbackState
                            stateBuilder.setState(state.state, position.toLong(), 1f)
                            setPlaybackState(stateBuilder.build())
                        }
                    }
                } catch (ignored: Exception) {
                }
            }
        }, 0, 1000)
    }

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(applicationContext, TAG)

        notificationManager = PlayerNotificationManager(mediaSession, this)

        stateBuilder = PlaybackStateCompat.Builder()
            .addCustomAction(EVENT_REWIND, "Rewind", R.drawable.ic_replay_30)
            .addCustomAction(EVENT_FAST_FORWARD, "Forward", R.drawable.ic_forward_30)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )

        metadataBuilder = MediaMetadataCompat.Builder()
        mediaSession.setMetadata(metadataBuilder!!.build())

        mediaSession.setCallback(MySessionCallback())
        sessionToken = mediaSession.sessionToken

        dao = getInstance(this).audiobookDao()!!
    }

    override fun onDestroy() {
        super.onDestroy()
        if (updateTask != null) {
            updateTask!!.cancel()
        }
        mediaPlayer.release()
        mediaSession.release()
    }

    private fun updateNotification() {
        val notification = notificationManager!!.updateNotification(isPlaying, bookId)
        startForeground(2, notification)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(ArrayList())
    }

    private inner class MySessionCallback : MediaSessionCompat.Callback() {
        override fun onCustomAction(action: String, extras: Bundle) {
            super.onCustomAction(action, extras)
            mediaSession.sendSessionEvent(action, extras)
            when (action) {
                EVENT_REACHED_END -> {
                    onStop()
                }

                EVENT_REWIND -> {
                    onRewind()
                }

                EVENT_FAST_FORWARD -> {
                    onFastForward()
                }
            }
        }

        override fun onPlay() {
            if (hasBeenInterrupted) {
                hasBeenInterrupted = false
                return
            }
            registerReceiver(broadcastReceiver, intentFilter)
            val am = this@MediaPlaybackService.getSystemService(AUDIO_SERVICE) as AudioManager
            if (getIsMediaPlayerPrepared() && !isMediaPlayerPlaying) {
                val result = request?.let { am.requestAudioFocus(it) }
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mediaSession.isActive = true
                    if (dao.getStatus(bookId) == AudioBook.STATUS_NOT_BEGUN) {
                        updateStatus(AudioBook.STATUS_IN_PROGRESS)
                    }
                    val newState = stateBuilder
                        .setState(
                            PlaybackStateCompat.STATE_PLAYING,
                            dao.getPositionInTrack(bookId).toLong(),
                            1f
                        )
                        .build()
                    setPlaybackState(newState)
                    mediaPlayer.start()
                    initialiseTimer()
                }
                updateNotification()
            }
        }

        override fun onSeekTo(pos: Long) {
            var pos = pos
            if (getIsMediaPlayerPrepared()) {
                if (pos < 1) {
                    pos = 1
                } else {
                    val duration = mediaPlayer.duration
                    if (pos > duration) {
                        pos = (duration - 1).toLong()
                    }
                }
                setPlaybackState(
                    stateBuilder
                        .setState(mediaSession.controller.playbackState.state, pos, 1f)
                        .build()
                )
                mediaPlayer.seekTo(pos.toInt())
            }
        }

        override fun onPause() {
            if (getIsMediaPlayerPrepared() && isMediaPlayerPlaying) {
                val newState = stateBuilder
                    .setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        mediaPlayer.currentPosition.toLong(),
                        1f
                    )
                    .build()
                setPlaybackState(newState)
                mediaPlayer.pause()
                updateNotification()
            }
        }

        override fun onStop() {
            unregisterReceiver(broadcastReceiver)
            if (getIsMediaPlayerPrepared()) {
                val am = this@MediaPlaybackService.getSystemService(AUDIO_SERVICE) as AudioManager
                am.abandonAudioFocusRequest(audioFocusRequest!!)
                mediaSession.isActive = false
                updateTask!!.cancel()
                val newState = stateBuilder
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1f)
                    .build()
                setPlaybackState(newState)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(intentId)
            }
        }

        override fun onFastForward() {
            super.onFastForward()
            mediaSession.controller.transportControls.seekTo(
                (mediaPlayer.currentPosition + 30 * 1000).toLong()
            )
        }

        override fun onRewind() {
            super.onRewind()
            mediaSession.controller.transportControls.seekTo(
                (mediaPlayer.currentPosition - 30 * 1000).toLong()
            )
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            if (getIsMediaPlayerPrepared()) {
                onPause()
                dao.updatePositionInTrack(bookId, 1, System.currentTimeMillis())
                setPlaybackState(
                    stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 1, 1f)
                        .build()
                )
                playTrack(dao.getPositionInTrackList(bookId) + 1)
            }
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            if (getIsMediaPlayerPrepared()) {
                if (mediaPlayer.currentPosition > 5 * 1000) {
                    mediaSession.controller.transportControls.seekTo(1)
                } else {
                    onPause()
                    dao.updatePositionInTrack(bookId, 1, System.currentTimeMillis())
                    setPlaybackState(
                        stateBuilder
                            .setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 1, 1f)
                            .build()
                    )
                    playTrack(dao.getPositionInTrackList(bookId) - 1)
                }
            }
        }
    }


    companion object {
        const val EVENT_REACHED_END: String = "EVENT_REACHED_END"
        private const val EVENT_REWIND = "EVENT_REWIND"
        private const val EVENT_FAST_FORWARD = "EVENT_FAST_FORWARD"
        private const val TAG = "ASD"
    }
}
