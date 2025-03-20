package com.kjjejones42.audiobookplayer;

import android.app.Application;
import android.content.Context;

public class AppApplication extends Application {
    private static AppApplication instance;

    public static Context getContext() {
        return instance;
    }

    public void onCreate () {
        instance = this;
        super.onCreate();
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        assert defaultHandler != null;
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Utils.logError(e, this);
            defaultHandler.uncaughtException(thread, e);
        });
    }
}