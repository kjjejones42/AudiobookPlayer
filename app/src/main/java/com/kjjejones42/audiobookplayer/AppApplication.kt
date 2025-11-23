package com.kjjejones42.audiobookplayer

import android.app.Application
import android.content.Context
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.kjjejones42.audiobookplayer.database.AudioBookFileAlbumArtFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppApplication : Application(), SingletonImageLoader.Factory {

    companion object {
        private lateinit var instance: AppApplication

        val context: Context
            get() = instance
    }

    override fun onCreate() {
        instance = this
        super.onCreate()
        val defaultHandler = checkNotNull(Thread.getDefaultUncaughtExceptionHandler())
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            Log.e("AppApplication", e.stackTraceToString())
            logError(e, this)
            defaultHandler.uncaughtException(thread, e)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(AudioBookFileAlbumArtFetcher.Factory())
            }
            .build()
    }
}