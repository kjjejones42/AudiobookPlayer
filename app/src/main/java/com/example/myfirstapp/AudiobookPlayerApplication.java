package com.example.myfirstapp;

import android.app.Application;

public class AudiobookPlayerApplication extends Application {

    public void onCreate () {
        super.onCreate();

        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Utils.getInstance().logError(e, this);
            defaultHandler.uncaughtException(thread, e);
        });
    }
}