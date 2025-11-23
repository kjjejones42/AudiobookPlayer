package com.kjjejones42.audiobookplayer

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppApplication : Application() {

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
            logError(e, this)
            defaultHandler.uncaughtException(thread, e)
        }
    }
}