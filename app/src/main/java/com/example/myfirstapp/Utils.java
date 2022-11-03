package com.example.myfirstapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    @SuppressLint("SimpleDateFormat")
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private final static File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "MyAudiobookPlayerLog.txt");

    public static String documentUriToFilePath(Uri uri) {
        try {
            String path = uri.getPath();
            assert path != null;
            String[] segments = path.split(":");
            String mainPath = segments[segments.length - 1];
            if (segments.length > 1) {
                segments = segments[segments.length - 2].split("/");
            }
            String storage = segments[segments.length - 1];
            String prefix = "primary".equals(storage) ? Environment.getExternalStorageDirectory().getPath() : "/storage/" + storage;
            return new File(prefix + "/" + mainPath).getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void logError(Throwable e, @Nullable Context context) {
        logError(e, "", context);
    }

    public static void logError(Throwable e, String message, @Nullable Context context) {
        synchronized (logFile) {
            try {
                FileOutputStream fos = new FileOutputStream(logFile, true);
                PrintWriter printWriter = new PrintWriter(fos);
                printWriter.write(simpleDateFormat.format(new Date()) + System.lineSeparator());
                printWriter.write("" + message + System.lineSeparator());
                e.printStackTrace(printWriter);
                printWriter.write(System.lineSeparator() + "--------------------" + System.lineSeparator());
                printWriter.close();
                if (context != null) {
                    Toast.makeText(context, "Uncaught exception written to log", Toast.LENGTH_SHORT).show();
                }
            } catch (FileNotFoundException ex) {
                try {
                    if (logFile.createNewFile()) {
                        logError(ex, message, context);
                    }
                } catch (IOException ei) {
                    ei.printStackTrace();
                }
            }
        }
    }
}
