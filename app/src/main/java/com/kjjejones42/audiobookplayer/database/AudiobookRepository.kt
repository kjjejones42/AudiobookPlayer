package com.kjjejones42.audiobookplayer.database

import com.kjjejones42.audiobookplayer.database.models.AudioBook
import com.kjjejones42.audiobookplayer.database.models.AudioBookStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudiobookRepository @Inject constructor(
    private val audiobookDao: AudiobookDao
) {
    fun allAndObserve() = audiobookDao.allAndObserve
    fun getAudioBook(audioBookId: String) = audiobookDao.findByName(audioBookId)

    fun updatePositionInTrack(audioBookId: String, positionInTrack: Int) =
        audiobookDao.updatePositionInTrack(audioBookId, positionInTrack, System.currentTimeMillis())

    fun updatePositionInBook(audioBookId: String, positionInTrackList: Int) =
        audiobookDao.updatePositionInTrackList(audioBookId, positionInTrackList)

    fun updatePositions(audioBookId: String, positionInTrackList: Int, positionInTrack: Int) =
        audiobookDao.updatePositions(audioBookId, positionInTrackList, positionInTrack, System.currentTimeMillis())

    fun update(book: AudioBook) = audiobookDao.update(book)

    fun updateStatus(audiobookId: String, status: AudioBookStatus) {
        audiobookDao.updatePositions(audiobookId, 0, 0, System.currentTimeMillis())
        audiobookDao.updateStatus(audiobookId, status.value, System.currentTimeMillis())
    }

    fun findByName(displayName: String?) = audiobookDao.findByName(displayName)

    fun updatePositionInTrackList(displayName: String?, positionInTrackList: Int) =
        audiobookDao.updatePositionInTrackList(displayName, positionInTrackList)

    fun getPositionInTrack(displayName: String?) = audiobookDao.getPositionInTrack(displayName)

    fun getPositionInTrackList(displayName: String?) = audiobookDao.getPositionInTrackList(displayName)

    fun getStatus(displayName: String?) = audiobookDao.getStatus(displayName)

    fun mostRecentBook() = audiobookDao.mostRecentBook

    fun allBaseDirs() = audiobookDao.allBaseDirs

    fun insertAll(books: List<AudioBook>) = audiobookDao.insertAll(books)

    fun delete(baseDir: String?) = audiobookDao.delete(baseDir)
}