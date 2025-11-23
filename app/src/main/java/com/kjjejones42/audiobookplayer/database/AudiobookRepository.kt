package com.kjjejones42.audiobookplayer.database

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookRepository @Inject constructor(
    private val audiobookDao: AudiobookDao
) {
    fun allAndObserve() = audiobookDao.allAndObserve

    fun getAudioBook(audioBookId: String) = audiobookDao.findByName(audioBookId)
}