package com.kjjejones42.audiobookplayer.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kjjejones42.audiobookplayer.database.AudiobookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    audiobookRepository: AudiobookRepository
) : ViewModel() {

    private val _audioBookId = MutableStateFlow<String?>(null)

    val audioBook = _audioBookId
        .map { audiobookRepository.getAudioBook(it!!) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setAudioBook(audioBookId: String) {
        _audioBookId.value = audioBookId
    }

}
