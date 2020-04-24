package com.example.myfirstapp;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class AudiobookPlayerApplication extends Application {

    private final File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "MyAudiobookPlayerLog.txt");

    public void onCreate () {
        super.onCreate();
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            handleUncaughtException(e);
            defaultHandler.uncaughtException(thread, e);
        });
    }

    public void handleUncaughtException (Throwable e) {
        synchronized (f) {
            e.printStackTrace();
            try {
                FileOutputStream fos = new FileOutputStream(f, true);
                PrintWriter p = new PrintWriter(fos);
                e.printStackTrace(p);
                p.write(System.lineSeparator());
                p.close();
            } catch (Exception ignored) {
            }
        }
    }
}