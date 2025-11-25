package com.kjjejones42.audiobookplayer.database

import com.kjjejones42.audiobookplayer.database.models.AudioBook
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookRepository @Inject constructor(
    private val audiobookDao: AudiobookDao
) {
    fun allAndObserve() = audiobookDao.allAndObserve


    fun findByNameAndObserve(bookId: String?) = audiobookDao.findByNameAndObserve(bookId)

    fun getAudioBook(audioBookId: String) = audiobookDao.findByName(audioBookId)

    fun updatePositionInBook(audioBookId: String, positionInTrackList: Int) {
        audiobookDao.updateStatus(audioBookId, AudioBook.Status.IN_PROGRESS.value, System.currentTimeMillis())
        audiobookDao.updatePositionInTrackList(audioBookId, positionInTrackList)
    }

    fun updatePositions(audioBookId: String, positionInTrackList: Int, positionInTrack: Int) =
        audiobookDao.updatePositions(audioBookId, positionInTrackList, positionInTrack, System.currentTimeMillis())

    fun updateStatus(audiobookId: String, status: AudioBook.Status) {
        audiobookDao.updateStatus(audiobookId, status.value, 0)
        audiobookDao.updatePositions(audiobookId, 0, 0, 0)
    }

    fun updatePositionInTrackList(displayName: String?, positionInTrackList: Int) =
        audiobookDao.updatePositionInTrackList(displayName, positionInTrackList)

    fun mostRecentBook() = audiobookDao.mostRecentBook
}