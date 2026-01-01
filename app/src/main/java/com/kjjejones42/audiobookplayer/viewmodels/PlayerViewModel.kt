package com.kjjejones42.audiobookplayer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kjjejones42.audiobookplayer.database.AudiobookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        audiobookRepository: AudiobookRepository,
    ) : ViewModel() {
        private val _audioBookId = MutableStateFlow<String?>(null)
        val trackNo = MutableStateFlow<Int?>(null)

        val audioBook =
            _audioBookId
                .filter { it != null }
                .map { audiobookRepository.getAudioBook(it!!) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Companion.WhileSubscribed(5000),
                    initialValue = null,
                )

        fun setAudioBook(audioBookId: String?) {
            if (audioBookId == null) return
            _audioBookId.value = audioBookId
        }

        fun setTrackNo(trackNo: Int?) {
            this.trackNo.value = trackNo
        }
    }
