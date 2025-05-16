package com.kjjejones42.audiobookplayer.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.AudioBookStatus
import com.kjjejones42.audiobookplayer.R
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance
import com.kjjejones42.audiobookplayer.display.DisplayListActivity
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

private fun msToMMSS(ms: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    if (hours > 0) {
        return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

class PlayActivity : AppCompatActivity() {
    private lateinit var spinner: Spinner
    private lateinit var prevButton: ImageButton
    private lateinit var rewindButton: ImageButton
    private lateinit var toggleButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var progressText: TextView
    private lateinit var durationText: TextView
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var seekBar: SeekBar
    private lateinit var imView: ImageView
    private var controller: MediaControllerCompat? = null
    private lateinit var model: PlayerViewModel
    private val audiobookDao = getInstance(this).audiobookDao()

    private val onSeekBarChangeListener: OnSeekBarChangeListener =
        object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                controller?.takeIf { fromUser }
                    ?.transportControls?.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        }

    private val onItemSelectedListener: AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                if (spinner.tag as Int != position) {
                    initialiseMediaSession(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

    private val controllerCallback: MediaControllerCompat.Callback =
        object : MediaControllerCompat.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadataCompat) {
                model.setMetadata(metadata)
                controller?.let { onPlaybackStateChanged(it.playbackState) }
            }

            override fun onSessionEvent(event: String, extras: Bundle) {
                super.onSessionEvent(event, extras)
                if (MediaPlaybackService.EVENT_REACHED_END == event) {
                    var audioBook = model.audioBook.value
                    mediaBrowser.disconnect()
                    audioBook?.let {
                        audioBook = it
                        it.setStatus(AudioBookStatus.FINISHED)
                        audiobookDao.update(it)
                    }
                    onBackPressed()
                }
            }

            @SuppressLint("SwitchIntDef")
            override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                when (state.state) {
                    PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_NONE -> {}
                    PlaybackStateCompat.STATE_ERROR -> Toast.makeText(
                        this@PlayActivity,
                        "Playback Error",
                        Toast.LENGTH_SHORT
                    ).show()

                    else -> {
                        model.setPosition(state.position)
                        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
                        model.setIsPlaying(isPlaying)
                    }
                }
            }
        }

    private val connectionCallbacks: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                this@PlayActivity.onConnected()
            }
        }

    private fun initialiseMediaSession(trackNo: Int) {
        val book = model.audioBook.value
        if (controller == null || book == null) return
        val intent = Intent(this, MediaPlaybackService::class.java)
        intent.putExtra(INTENT_AUDIOBOOK, book.displayName)
        intent.putExtra(INTENT_INDEX, trackNo)
        startService(intent)
    }

    private fun setImage(bitmap: Bitmap) {
        imView.setImageBitmap(bitmap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_play)

        mediaBrowser = MediaBrowserCompat(this, ComponentName(this, MediaPlaybackService::class.java), connectionCallbacks, null)

        prevButton = findViewById(R.id.prevButton)
        rewindButton = findViewById(R.id.rewindButton)
        toggleButton = findViewById(R.id.toggleButton)
        forwardButton = findViewById(R.id.forwardButton)
        nextButton = findViewById(R.id.nextButton)
        progressText = findViewById(R.id.progress_text)
        durationText = findViewById(R.id.duration_text)
        seekBar = findViewById(R.id.seekBar)
        spinner = findViewById(R.id.trackChooser)
        imView = findViewById(R.id.albumArtView)

        model = ViewModelProvider(this)[PlayerViewModel::class.java]
        initializeModelObservers()

        setControlsEnabled(false)
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)
        intent?.let { onNewIntent(it) }
    }

    private fun initializeModelObservers() {
        model.isPlaying.observe(this) { this.onIsPlayingSet(it) }
        model.position.observe(this) { this.onPositionSet(it) }
        model.metadata.observe(this) { this.onMetadataSet(it) }
        model.audioBook.observe(this) { this.onAudioBookSet(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val shouldStart = intent.getBooleanExtra(DisplayListActivity.INTENT_START_PLAYBACK, false)
        val newBookId = intent.getSerializableExtra(
            DisplayListActivity.INTENT_PLAY_FILE,
            String::class.java
        )
        model.setStartPlayback(shouldStart)
        audiobookDao.findByName(newBookId)?.let { model.setAudioBook(it) }
    }

    private fun onIsPlayingSet(isPlaying: Boolean) {
        val image = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        toggleButton.setImageResource(image)
    }

    private fun setDuration(duration: Long) {
        if (duration > 0) {
            seekBar.max = duration.toInt()
            durationText.text = msToMMSS(duration)
        }
    }

    private fun onPositionSet(position: Long) {
        if (position > 0) {
            seekBar.progress = position.toInt()
            progressText.text = msToMMSS(position)
        }
    }

    private fun updateButtonColor(color: Int) {
        val list = listOf(prevButton, rewindButton, toggleButton, nextButton, forwardButton)
        list.forEach { it.background?.setTint(color) }
        supportActionBar?.setBackgroundDrawable(color.toDrawable())
        window.statusBarColor = ColorUtils.blendARGB(color, Color.BLACK, 0.25f)
        seekBar.thumb.setTint(color)
        seekBar.progressDrawable.setTint(color)
    }

    private fun updateStatusBarColor(color: Int) {
        supportActionBar?.setBackgroundDrawable(color.toDrawable())
        window.statusBarColor = ColorUtils.blendARGB(color, Color.BLACK, 0.25f)
    }

    private fun onMetadataSet(metadata: MediaMetadataCompat) {
        val duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        if (duration != 0L) {
            setDuration(duration)
            setImage(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
            val position = metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER).toInt()
            spinner.tag = position
            spinner.setSelection(position)
        }
    }

    private fun buildTransportControls() {
        controller?.let {
            val controller = it
            it.registerCallback(controllerCallback)
            toggleButton.setOnClickListener {
                if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
            prevButton.setOnClickListener { controller.transportControls.skipToPrevious() }
            nextButton.setOnClickListener {
                controller.transportControls.skipToNext()
                model.setPosition(0)
            }
            rewindButton.setOnClickListener { controller.transportControls.rewind() }
            forwardButton.setOnClickListener { controller.transportControls.fastForward() }
            setControlsEnabled(true)
        }
    }

    private fun setControlsEnabled(on: Boolean) {
        seekBar.visibility = if (on) View.VISIBLE else View.INVISIBLE
        seekBar.isEnabled = on
        nextButton.isEnabled = on
        prevButton.isEnabled = on
        toggleButton.isEnabled = on
        rewindButton.isEnabled = on
        forwardButton.isEnabled = on
    }

    private fun onAudioBookSet(book: AudioBook?) {
        book?.let {
            setColorFromAlbumArt(it)
            supportActionBar?.title = it.displayName
            val positionInTrackList = it.positionInTrackList
            it.files?.let {
                val sortedFiles = it.sorted().toList()
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortedFiles)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
                spinner.onItemSelectedListener = onItemSelectedListener
                val position = min((sortedFiles.size - 1), positionInTrackList)
                spinner.tag = position
                spinner.setSelection(position)

            }
        }
    }

    private fun setColorFromAlbumArt(book: AudioBook?) {
        book?.let {
            setImage(it.getAlbumArt(this))
            if (!it.isArtGenerated) {
                book.getAlbumArtPalette(this)?.let {
                    val nightMode =
                        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    val backColor = if (nightMode) {
                        it.getDarkMutedColor(Color.TRANSPARENT)
                    } else {
                        it.getLightMutedColor(Color.TRANSPARENT)
                    }
                    findViewById<View>(R.id.playerBackground).setBackgroundColor(backColor)
                    val color = it.getVibrantColor(resources.getColor(R.color.colorAccent, theme))
                    updateButtonColor(color)
                    updateStatusBarColor(color)
                }
            } else {
                val tv = TypedValue()
                theme.resolveAttribute(R.attr.colorAccent, tv, true)
                updateButtonColor(tv.data)
                theme.resolveAttribute(R.attr.colorPrimary, tv, true)
                updateStatusBarColor(tv.data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.updateBookFromDatabase(this)
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
        model.audioBook.value?.let {
            model.setPosition(it.positionInTrack.toLong())
            setDuration(it.durationOfMostRecentTrack)
        }
    }

    private fun onConnected() {
        controller = MediaControllerCompat(this@PlayActivity, mediaBrowser.sessionToken)
        val controller = checkNotNull(controller)
        MediaControllerCompat.setMediaController(this@PlayActivity, controller)
        buildTransportControls()
        model.audioBook.value?.let{
            if (it.uniqueId != controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)) {
                initialiseMediaSession(it.positionInTrackList)
            } else {
                model.setMetadata(controller.metadata)
                model.setPosition(controller.playbackState.position)
                model.setIsPlaying(controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING)
            }
        }
        model.startPlayback.value?.takeIf { it }.also {
            model.setStartPlayback(false)
            controller.transportControls.play()
        }
    }

    override fun onStop() {
        super.onStop()
        controller?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }

    companion object {
        const val INTENT_AUDIOBOOK: String = "AUDIOBOOK"
        const val INTENT_INDEX: String = "INDEX"
    }
}
