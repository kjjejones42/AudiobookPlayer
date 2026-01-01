package com.kjjejones42.audiobookplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(entities = [AudioBook::class], version = 1)
@TypeConverters(DataConverter::class)
abstract class AudiobookDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var instance: AudiobookDatabase? = null

//                .allowMainThreadQueries()
//                .setQueryCallback({
//                    sqlQuery, bindArgs ->
//                    println("SQL Query: $sqlQuery SQL Args: $bindArgs")
//                }, executor = Executors.newSingleThreadExecutor())

        private fun buildDatabase(context: Context) = databaseBuilder(context, AudiobookDatabase::class.java, "audiobook_database").build()

        fun getInstance(context: Context): AudiobookDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
    }

    abstract fun audiobookDao(): AudiobookDao

    @Module
    @InstallIn(SingletonComponent::class)
    object DatabaseModule {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): AudiobookDatabase = getInstance(context)

        @Provides
        fun provideAudiobookDao(database: AudiobookDatabase): AudiobookDao = database.audiobookDao()
    }
}
