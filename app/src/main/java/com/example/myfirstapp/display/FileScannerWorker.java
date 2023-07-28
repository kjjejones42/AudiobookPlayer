package com.example.myfirstapp.display;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myfirstapp.AudioBook;
import com.example.myfirstapp.MediaItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private static final List<Integer> authorKeys = Arrays.asList(MediaMetadataRetriever.METADATA_KEY_ARTIST, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, MediaMetadataRetriever.METADATA_KEY_AUTHOR, MediaMetadataRetriever.METADATA_KEY_COMPOSER, MediaMetadataRetriever.METADATA_KEY_WRITER);

    public FileScannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    private String findAuthor(Collection<MediaItem> mediaFiles) {
        String author = null;
        try {
            MediaMetadataRetriever m = new MediaMetadataRetriever();
            for (MediaItem item : mediaFiles) {
                m.setDataSource(item.filePath);
                for (Integer i : authorKeys) {
                    author = m.extractMetadata(i);
                    if (author != null) {
                        m.close();
                        return author;
                    }
                }
            }
        } catch (IOException ignored) {}
        return author;
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
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DURATION
                },
                MediaStore.Audio.Media.IS_AUDIOBOOK + " = ?",
                new String[]{ "1" },
                null
        );
        if (cursor != null) {
            int DATA = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int DIR = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH);
            int TITLE = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int DURATION = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            Map<String, List<MediaItem>> dirs = new HashMap<>();
            while (cursor.moveToNext()) {
                String file = cursor.getString(DATA);
                String dir = cursor.getString(DIR);
                int duration = cursor.getInt(DURATION);
                String title = cursor.getString(TITLE);
                MediaItem media = new MediaItem(file, title, duration);
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
            return Result.failure();
        }
        return Result.success();
    }
}

