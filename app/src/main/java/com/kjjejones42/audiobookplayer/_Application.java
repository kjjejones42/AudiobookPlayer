package com.kjjejones42.audiobookplayer;

import android.app.Application;

public class _Application extends Application {

    public void onCreate () {
        super.onCreate();
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        assert defaultHandler != null;
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Utils.logError(e, this);
            defaultHandler.uncaughtException(thread, e);
        });
    }
}