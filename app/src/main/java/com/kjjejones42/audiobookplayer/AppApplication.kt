package com.kjjejones42.audiobookplayer

import android.app.Application
import android.content.Context

class AppApplication : Application() {
    override fun onCreate() {
        instance = this
        super.onCreate()
        val defaultHandler = checkNotNull(Thread.getDefaultUncaughtExceptionHandler())
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            logError(e, this)
            defaultHandler.uncaughtException(thread, e)
        }
    }

    companion object {
        private lateinit var instance: AppApplication

        val context: Context
            get() = instance
    }
}