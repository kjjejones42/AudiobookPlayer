package com.kjjejones42.audiobookplayer.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.kjjejones42.audiobookplayer.AudioBook;

@Database(entities = { AudioBook.class }, version = 1)
@TypeConverters({ DataConverter.class })
 public abstract class AudiobookDatabase extends RoomDatabase {

    private static AudiobookDatabase instance;

    static public AudiobookDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context, AudiobookDatabase.class, "audiobook_database")
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    public abstract AudiobookDao audiobookDao();
}
