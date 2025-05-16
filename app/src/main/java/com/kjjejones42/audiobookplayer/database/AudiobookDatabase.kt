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

    companion object {
        @Volatile
        private var instance: AudiobookDatabase? = null

        private fun buildDatabase(context: Context): AudiobookDatabase {
            return databaseBuilder(context, AudiobookDatabase::class.java, "audiobook_database")
                .allowMainThreadQueries()
                .build()
        }

        fun getInstance(context: Context): AudiobookDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
    }

    abstract fun audiobookDao(): AudiobookDao
}
