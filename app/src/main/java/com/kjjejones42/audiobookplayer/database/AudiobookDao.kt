package com.kjjejones42.audiobookplayer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {
    @get:Query("SELECT * FROM AudioBook")
    val allAndObserve: Flow<List<AudioBook>>

    @Query("UPDATE AudioBook SET status = :status, lastSavedTimestamp = :currentTimeStamp WHERE displayName = :displayName")
    suspend fun updateStatus(
        displayName: String,
        status: Int,
        currentTimeStamp: Long,
    )

    @Query("SELECT * FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    suspend fun findByName(displayName: String?): AudioBook?

    @Query("SELECT * FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    fun findByNameAndObserve(displayName: String?): Flow<AudioBook?>

    @Query("UPDATE AudioBook SET positionInTrackList = :positionInTrackList WHERE displayName = :displayName")
    suspend fun updatePositionInTrackList(
        displayName: String?,
        positionInTrackList: Int,
    )

    @Query(
        "UPDATE AudioBook SET positionInTrackList = :positionInTrackList, positionInTrack = :positionInTrack, lastSavedTimestamp = :currentTimeStamp WHERE displayName = :displayName",
    )
    suspend fun updatePositions(
        displayName: String?,
        positionInTrackList: Int,
        positionInTrack: Int,
        currentTimeStamp: Long,
    )

    @Query("SELECT * FROM AudioBook ORDER BY lastSavedTimestamp DESC LIMIT 1")
    suspend fun getMostRecentBook(): AudioBook?

    @get:Query("SELECT baseDir FROM AudioBook")
    val allBaseDirs: List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(books: List<AudioBook>)

    @Query("DELETE FROM AudioBook WHERE baseDir = :baseDir")
    fun delete(baseDir: String?)
}
