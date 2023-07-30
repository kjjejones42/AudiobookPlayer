package com.kjjejones42.audiobookplayer.display;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.MediaItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileScannerWorker extends Worker {

    static final String LIST_OF_DIRS = "LIST_OF_DIRS";

    private static final String[] authorFields = new String[]{
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.AUTHOR,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.WRITER
    };

    public FileScannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private String getFileAuthor(Uri filename) {
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                authorFields,
                MediaStore.Audio.Media._ID + " = ?",
                new String[]{ filename.getLastPathSegment() },
                null
        )){
            if (cursor == null) return null;
            while (cursor.moveToNext()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    String result = cursor.getString(i);
                    if (result != null && !result.isEmpty() && !result.equals("<unknown>")) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private String findAuthor(Collection<MediaItem> mediaFiles) {
        for (MediaItem item : mediaFiles) {
            String author = getFileAuthor(item.getUri());
            if (author != null) {
                return author;
            }
        }
        return "";
    }

    @SuppressLint("Range")
    private String findImage(String directory) {
        Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{ MediaStore.Images.Media.DATA },
                MediaStore.Images.Media.RELATIVE_PATH + " = ?",
                new String[]{ directory },
                null
        );
        String result = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return result;
    }

    private AudioBook parseBook(String rel, List<MediaItem> mediaFiles) {
        String directory = new File(rel).getName();
        String imagePath = findImage(rel);
        String author = findAuthor(mediaFiles);
        return new AudioBook(directory, imagePath, mediaFiles, author);

    }

    @SuppressLint("Range")
    private List<AudioBook> getList() {
        List<AudioBook> results = new ArrayList<>();

        Cursor cursor = getApplicationContext().getContentResolver().query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DURATION,
                },
                MediaStore.Audio.Media.IS_AUDIOBOOK + " != 0",
                null,
                null
        );
        if (cursor != null) {
            Map<String, List<MediaItem>> dirs = new HashMap<>();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String dir = cursor.getString(1);
                String title = cursor.getString(2);
                int duration = cursor.getInt(3);
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                MediaItem media = new MediaItem(uri, title, duration);
                if (!dirs.containsKey(dir)) {
                    dirs.put(dir, new ArrayList<>());
                }
                Objects.requireNonNull(dirs.get(dir)).add(media);
            }
            cursor.close();
            for (Map.Entry<String, List<MediaItem>> i : dirs.entrySet()) {
                AudioBook result = parseBook(i.getKey(), i.getValue());
                results.add(result);
            }
        }
        return results;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<AudioBook> results = getList();
            FileOutputStream fos = getApplicationContext().openFileOutput(LIST_OF_DIRS, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(results);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
        return Result.success();
    }
}

