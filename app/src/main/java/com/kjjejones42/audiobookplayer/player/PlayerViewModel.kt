package com.kjjejones42.audiobookplayer.player

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance

class PlayerViewModel : ViewModel() {
    private val _isPlaying = MutableLiveData<Boolean>()
    private val _position = MutableLiveData<Long>()
    private val _metadata = MutableLiveData<MediaMetadataCompat>()
    private val _audioBook = MutableLiveData<AudioBook?>()
    private val _startPlayback = MutableLiveData<Boolean>()

    val isPlaying: LiveData<Boolean> get() = _isPlaying
    val audioBook: LiveData<AudioBook?> get() = _audioBook
    val position: LiveData<Long> get() = _position
    val metadata: LiveData<MediaMetadataCompat> get() = _metadata
    val startPlayback: LiveData<Boolean> get() = _startPlayback

    fun setAudioBook(audioBook: AudioBook?) {
        _audioBook.value = audioBook
    }

    fun setStartPlayback(startPlayback: Boolean) {
        val b = _startPlayback.value
        if (b != null && b != startPlayback) {
            _startPlayback.value = startPlayback
        }
    }

    init {
        setIsPlaying(true)
        clear()
    }

    private val emptyMetadata: MediaMetadataCompat
        get() {
            if (Companion.emptyMetadata == null) {
                Companion.emptyMetadata = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "")
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0L)
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 0)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                    .build()
            }
            return Companion.emptyMetadata!!
        }

    private fun clear() {
        _position.value = 0L
        _metadata.value = emptyMetadata
    }

    fun setIsPlaying(isPlaying: Boolean) {
        val b = this.isPlaying.value
        if (b != null && b != isPlaying) {
            _isPlaying.value = isPlaying
        }
    }

    fun setPosition(position: Long) {
        val l = this.position.value
        if (l != null && l != position) {
            if (position > 0) {
                _position.value = position
            }
        }
    }

    fun updateBookFromDatabase(context: Context) {
        var book = audioBook.value
        if (book != null) {
            val bookId = audioBook.value!!.displayName
            book = getInstance(context).audiobookDao()!!.findByName(bookId)
            setAudioBook(book)
        }
    }

    fun setMetadata(metadata: MediaMetadataCompat) {
        _metadata.value = metadata
    }

    companion object {
        private var emptyMetadata: MediaMetadataCompat? = null
    }
}
