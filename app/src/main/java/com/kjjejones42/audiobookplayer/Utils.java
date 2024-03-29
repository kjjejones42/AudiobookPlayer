package com.kjjejones42.audiobookplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
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
