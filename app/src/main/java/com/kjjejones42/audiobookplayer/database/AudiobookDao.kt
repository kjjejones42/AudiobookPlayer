package com.kjjejones42.audiobookplayer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kjjejones42.audiobookplayer.AudioBook
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {
    @get:Query("SELECT * FROM AudioBook")
    val allAndObserve: Flow<List<AudioBook>>

    @Update
    fun update(book: AudioBook)

    @Query("SELECT * FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    fun findByName(displayName: String?): AudioBook?

    @Query("UPDATE AudioBook SET positionInTrack = :positionInTrack, lastSavedTimestamp = :currentTimeStamp WHERE displayName = :displayName")
    fun updatePositionInTrack(displayName: String?, positionInTrack: Int, currentTimeStamp: Long)

    @Query("UPDATE AudioBook SET positionInTrackList = :positionInTrackList WHERE displayName = :displayName")
    fun updatePositionInTrackList(displayName: String?, positionInTrackList: Int)

    @Query("SELECT positionInTrack FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    fun getPositionInTrack(displayName: String?): Int

    @Query("SELECT positionInTrackList FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    fun getPositionInTrackList(displayName: String?): Int

    @Query("SELECT status FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    fun getStatus(displayName: String?): Int

    @get:Query("SELECT * FROM AudioBook ORDER BY lastSavedTimestamp DESC LIMIT 1")
    val mostRecentBook: AudioBook?

    @get:Query("SELECT baseDir FROM AudioBook")
    val allBaseDirs: List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(books: List<AudioBook>)


    @Query("DELETE FROM AudioBook WHERE baseDir = :baseDir")
    fun delete(baseDir: String?)
}
