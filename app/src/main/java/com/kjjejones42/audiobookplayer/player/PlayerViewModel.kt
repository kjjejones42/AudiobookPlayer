package com.kjjejones42.audiobookplayer.player

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance

class PlayerViewModel : ViewModel() {

    companion object {
        private val emptyMetadata: MediaMetadataCompat by lazy {
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "")
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0L)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 0)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                .build()
        }
    }

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private val _position = MutableLiveData<Long>()
    val position: LiveData<Long> get() = _position

    private val _metadata = MutableLiveData<MediaMetadataCompat>()
    val metadata: LiveData<MediaMetadataCompat> get() = _metadata

    private val _audioBook = MutableLiveData<AudioBook?>()
    val audioBook: LiveData<AudioBook?> get() = _audioBook

    private val _startPlayback = MutableLiveData<Boolean>()
    val startPlayback: LiveData<Boolean> get() = _startPlayback

    fun setAudioBook(audioBook: AudioBook?) {
        _audioBook.value = audioBook
    }

    fun setStartPlayback(startPlayback: Boolean) {
        if (startPlayback != _startPlayback.value) {
            _startPlayback.value = startPlayback
        }
    }

    fun setMetadata(metadata: MediaMetadataCompat) {
        _metadata.value = metadata
    }

    init {
        setIsPlaying(true)
        clear()
    }

    private fun clear() {
        _position.value = 0L
        _metadata.value = emptyMetadata
    }

    fun setIsPlaying(isPlaying: Boolean) {
        if (isPlaying != _isPlaying.value) {
            _isPlaying.value = isPlaying
        }
    }

    fun setPosition(position: Long) {
        if (position > 0 && this.position.value != position) {
            _position.value = position
        }
    }

    fun updateBookFromDatabase(context: Context) {
        audioBook.value?.displayName?.let {
            val book = getInstance(context).audiobookDao().findByName(it)
            book?.let { setAudioBook(it) }
        }
    }
}
