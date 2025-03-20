package com.kjjejones42.audiobookplayer;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {
    @SuppressLint("SimpleDateFormat")
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    public static void logError(Throwable e, @Nullable Context context) {
        logError(e, "", context);
    }

    public static void logError(Throwable e, String message, @Nullable Context context) {
        writeToFile(e, message, context);
    }

    public static void writeToFile(Throwable e, String message, @Nullable Context context) {
        try {
            if (context == null) context = AppApplication.getContext();

            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm", Locale.getDefault());
            df.setTimeZone(tz);
            String nowAsISO = df.format(new Date());

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "AudioBookPlayerError - " + nowAsISO + ".log");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, ": text/plain");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/" + context.getPackageName());
            ContentResolver resolver = context.getContentResolver();

            Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
            assert uri != null;
            OutputStream fos = resolver.openOutputStream(uri);
            assert fos != null;
            PrintWriter printWriter = new PrintWriter(fos);
            printWriter.write(simpleDateFormat.format(new Date()) + System.lineSeparator());
            printWriter.write(message + System.lineSeparator());
            e.printStackTrace(printWriter);
            printWriter.write(System.lineSeparator() + "--------------------" + System.lineSeparator());
            printWriter.close();
            Toast.makeText(context, "Uncaught exception written to log", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}