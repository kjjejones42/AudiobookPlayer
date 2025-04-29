package com.kjjejones42.audiobookplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {
    private static Uri logFileUri;
    public static void logError(Throwable e, @Nullable Context context) {
        logError(e, "", context);
    }

    public static void logError(Throwable e, String message, @Nullable Context context) {
        writeToFile(e, message, context);
    }

    private static void writeToFile(Throwable e, String message, @Nullable Context context) {
        try {
            if (context == null) {
                context = AppApplication.getContext();
            }
            Uri uri = getLogFileUri(context);
            assert uri != null;
            OutputStream fos = context.getContentResolver().openOutputStream(uri, "wa");
            assert fos != null;
            PrintWriter printWriter = new PrintWriter(fos);
            printWriter.write(currentDateTime() + "\n");
            printWriter.write(message + "\n");
            e.printStackTrace(printWriter);
            printWriter.write("\n--------------------\n");
            printWriter.close();
            Toast.makeText(context, "Uncaught exception written to log", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Uri getLogFileUri(Context context) {
        if (logFileUri == null) {
            Uri uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
            String fileName = "AudioBookPlayerError.log";
            String relativePath = String.format("%s/%s/", Environment.DIRECTORY_DOCUMENTS, context.getPackageName());
            ContentResolver resolver = context.getContentResolver();
            logFileUri = queryLogFileUri(resolver, uri, fileName, relativePath);
            if (logFileUri == null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
                logFileUri = resolver.insert(uri, contentValues);
            }
        }
        return logFileUri;
    }

    private static String currentDateTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z", Locale.getDefault());
        return format.format(new Date());
    }

    private static Uri queryLogFileUri(ContentResolver resolver, Uri uri, String fileName, String relativePath) {
        String[] selection = new String[] { MediaStore.MediaColumns._ID };
        String where = String.format("%s = ? AND %s = ?", MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH);
        String[] args = new String[]{ fileName, relativePath };
        try (Cursor cursor = resolver.query(uri, selection, where, args, null)) {
            if (cursor != null && cursor.moveToNext()) {
                long id = cursor.getLong(0);
                return ContentUris.withAppendedId(uri, id);
            }
        }
        return null;
    }
}