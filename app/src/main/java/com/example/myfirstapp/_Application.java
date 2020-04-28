package com.example.myfirstapp;

import android.app.Application;

public class _Application extends Application {

    public void onCreate () {
        super.onCreate();

        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Utils.logError(e, this);
            defaultHandler.uncaughtException(thread, e);
        });
    }
}