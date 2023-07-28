package com.kjjejones42.audiobookplayer.display;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.MediaItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FileScannerWorker extends Worker {

    static final String LIST_OF_DIRS = "LIST_OF_DIRS";
    private static final List<Integer> authorKeys = Arrays.asList(
            MediaMetadataRetriever.METADATA_KEY_ARTIST,
            MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
            MediaMetadataRetriever.METADATA_KEY_AUTHOR,
            MediaMetadataRetriever.METADATA_KEY_COMPOSER,
            MediaMetadataRetriever.METADATA_KEY_WRITER
    );

    public FileScannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private String getFileAuthor(String filename, MediaMetadataRetriever m) {
        try {
            if (filename != null && new File(filename).exists()) {
                m.setDataSource(filename);
                for (Integer i : authorKeys) {
                    String author = m.extractMetadata(i);
                    m.close();
                    return author;
                }
            }

        } catch (Exception ignored) {}
        return null;
    }

    private String findAuthor(Collection<MediaItem> mediaFiles) {
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        for (MediaItem item : mediaFiles) {
            String author = getFileAuthor(item.filePath, m);
            if (author != null) {
                return author;
            }
        }
        return null;
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
        File directory = new File(mediaFiles.get(0).filePath).getParentFile();
        assert directory != null;
        String directoryPath = directory.getAbsolutePath();
        String imagePath = findImage(rel);
        String author = findAuthor(mediaFiles);
        return new AudioBook(directory.getName(), directoryPath, imagePath, mediaFiles, author);

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
                    MediaStore.Audio.Media.DATA
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
                String file = cursor.getString(4);
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                MediaItem media = new MediaItem(file, uri.toString(), title, duration);
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
            Log.e("ASD", e.toString());
            e.printStackTrace();
            return Result.failure();
        }
        return Result.success();
    }
}

