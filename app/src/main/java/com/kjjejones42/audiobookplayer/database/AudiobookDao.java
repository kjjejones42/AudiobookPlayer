package com.kjjejones42.audiobookplayer.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.kjjejones42.audiobookplayer.AudioBook;

import java.util.List;

@Dao
public interface AudiobookDao {
    @Query("SELECT * FROM AudioBook")
    List<AudioBook> getAll();

    @Query("SELECT * FROM AudioBook")
    LiveData<List<AudioBook>> getAllAndObserve();

    @Update
    void update(AudioBook book);

    @Query("SELECT * FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    AudioBook findByName(String displayName);

    @Query("UPDATE AudioBook SET positionInTrack = :positionInTrack, lastSavedTimestamp = :currentTimeStamp WHERE displayName = :displayName")
    void updatePositionInTrack(String displayName, int positionInTrack, long currentTimeStamp);

    @Query("UPDATE AudioBook SET positionInTrackList = :positionInTrackList WHERE displayName = :displayName")
    void updatePositionInTrackList(String displayName, int positionInTrackList);

    @Query("SELECT positionInTrack FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    int getPositionInTrack(String displayName);

    @Query("SELECT positionInTrackList FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    int getPositionInTrackList(String displayName);

    @Query("SELECT status FROM AudioBook WHERE displayName = :displayName LIMIT 1")
    int getStatus(String displayName);

    @Query("SELECT * FROM AudioBook ORDER BY lastSavedTimestamp DESC LIMIT 1")
    AudioBook getMostRecentBook();

    @Insert
    void insertAll(List<AudioBook> books);
}
