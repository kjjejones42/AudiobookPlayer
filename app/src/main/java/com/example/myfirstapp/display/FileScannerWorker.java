package com.example.myfirstapp.display;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myfirstapp.AudioBook;
import com.example.myfirstapp.MediaItem;
import com.example.myfirstapp.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileScannerWorker extends Worker {

    static final String INPUT = "INPUT";
    static final String LIST_OF_DIRS = "LIST_OF_DIRS";
    private static final List<Integer> authorKeys = Arrays.asList(MediaMetadataRetriever.METADATA_KEY_ARTIST, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, MediaMetadataRetriever.METADATA_KEY_AUTHOR, MediaMetadataRetriever.METADATA_KEY_COMPOSER, MediaMetadataRetriever.METADATA_KEY_WRITER);
    private final Context context;

    private static class AudioBookResult {
        String imageUri;
        List<MediaItem> media;
        String author;
    }

    private final List<String> audioFormats = Arrays.asList(
            MediaFormat.MIMETYPE_AUDIO_AAC, MediaFormat.MIMETYPE_AUDIO_AC3,
            MediaFormat.MIMETYPE_AUDIO_AMR_NB, MediaFormat.MIMETYPE_AUDIO_AMR_WB,
            MediaFormat.MIMETYPE_AUDIO_FLAC, MediaFormat.MIMETYPE_AUDIO_G711_ALAW,
            MediaFormat.MIMETYPE_AUDIO_G711_MLAW, MediaFormat.MIMETYPE_AUDIO_MPEG,
            MediaFormat.MIMETYPE_AUDIO_MSGSM, MediaFormat.MIMETYPE_AUDIO_OPUS,
            MediaFormat.MIMETYPE_AUDIO_QCELP, MediaFormat.MIMETYPE_AUDIO_RAW,
            MediaFormat.MIMETYPE_AUDIO_VORBIS);


    private boolean isAudio(String input) {
        return audioFormats.contains(input);
    }

    private String getMimeType(File file) {
        try {
            String[] filename = file.getName().split("\\.");
            String extension = filename[filename.length - 1];
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        } catch (Exception e) {
            return "";
        }
    }

    private AudioBookResult getAudioInDirectoryFile(File directory) {
        List<MediaItem> list = new ArrayList<>();
        String imageUri = null;
        String author = null;
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        for (File file : directory.listFiles()) {
            String type = getMimeType(file);
            if (isAudio(type)) {
                long duration = 0L;
                try {
                    m.setDataSource(file.getPath());
                    duration = Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    if (author == null) {
                        for (Integer i : authorKeys) {
                            author = m.extractMetadata(i);
                            if (author != null) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                MediaItem newItem = new MediaItem(file.getPath(), file.getName(), duration);
                list.add(newItem);
            } else if (!file.isDirectory() && imageUri == null) {
                try {
                    FileInputStream fis =  new FileInputStream(file);
                    if (BitmapFactory.decodeStream(fis) != null) {
                        imageUri = file.getPath();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        AudioBookResult result = new AudioBookResult();
        result.media = list;
        result.imageUri = imageUri;
        result.author = author;
        return result;
    }

    private final BlockingQueue<String> taskQueue = new ArrayBlockingQueue<>(50);
    private final ExecutorService pool = Executors.newFixedThreadPool(10);
    private final List<Future<AudioBook>> results = Collections.synchronizedList(new ArrayList<>());

    private List<AudioBook> getList(String initialPath) {
        taskQueue.offer(initialPath);
        while (!isAllDone(results) || !taskQueue.isEmpty()) {
            try {
                String path = taskQueue.poll();
                if (path != null) {
                    Future<AudioBook> f = pool.submit(() -> checkDirectoryFile(path));
                    results.add(f);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<AudioBook> books = new ArrayList<>();
        for (Future<AudioBook> result : results) {
            try {
                AudioBook book = result.get();
                if (book != null){
                    books.add(book);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        pool.shutdown();
        return books;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Uri root = Uri.parse(getInputData().getString(INPUT));
            File rootDir = new File(Utils.documentUriToFilePath(root));
            List<AudioBook> result = getList(rootDir.getPath());
            FileOutputStream fos = getApplicationContext().openFileOutput(LIST_OF_DIRS, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(result);
            oos.close();
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.logError(e, context);
            return Result.failure();
        }
    }

    private <T> boolean isAllDone(List<Future<T>> list) {
        for (Future future : list) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    private AudioBook checkDirectoryFile(String filePath) {
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        for (File child : file.listFiles()) {
            if (child != null && child.isDirectory()) {
                boolean added = false;
                while (!added) {
                    added = taskQueue.offer(child.getPath());
                }
            }
        }
        AudioBookResult result = getAudioInDirectoryFile(file);
        if (!result.media.isEmpty()) {
            return new AudioBook(file.getName(), filePath, result.imageUri, result.media, result.author);
        }
        return null;
    }


    public FileScannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }
}

