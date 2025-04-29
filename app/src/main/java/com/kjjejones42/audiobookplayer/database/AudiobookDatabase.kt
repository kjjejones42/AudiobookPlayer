package com.kjjejones42.audiobookplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kjjejones42.audiobookplayer.AudioBook

@Database(entities = [AudioBook::class], version = 1)
@TypeConverters(DataConverter::class)
abstract class AudiobookDatabase : RoomDatabase() {
    abstract fun audiobookDao(): AudiobookDao

    companion object {
        private var instance: AudiobookDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): AudiobookDatabase {
            if (instance != null) {
                return instance!!
            }
            synchronized(AudiobookDatabase::class.java) {
                if (instance == null) {
                    instance = databaseBuilder(
                        context,
                        AudiobookDatabase::class.java, "audiobook_database"
                    )
                        .allowMainThreadQueries()
                        .build()
                }
            }
            return instance!!
        }
    }
}
