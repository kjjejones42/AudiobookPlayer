package com.example.myfirstapp.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myfirstapp.AudioBook;
import com.example.myfirstapp.MediaItem;
import com.example.myfirstapp.Utils;

import java.io.FileOutputStream;
import java.io.InputStream;
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

    private final ContentResolver resolver = getApplicationContext().getContentResolver();
    private Context context;

    private class AudioBookResult {
        String imageUri;
        List<MediaItem> media;
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

    private boolean isDirectory(String input) {
        return input.equals(DocumentsContract.Document.MIME_TYPE_DIR);
    }

    private AudioBookResult getAudioInDirectory(Uri root, String id) {
        List<MediaItem> list = new ArrayList<>();
        String imageUri = null;
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(root, id);
        Cursor cursor = resolver.query(uri, null, null, null, null);
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String childId = cursor.getString(cursor.getColumnIndex("document_id"));
                String type = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
                Uri newUri = DocumentsContract.buildDocumentUriUsingTree(root, childId);
                if (isAudio(type)) {
                    String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                    long duration = 0L;
                    try {
                        m.setDataSource(context, newUri);
                        duration = Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    MediaItem newItem = new MediaItem(newUri.toString(), displayName, duration);
                    list.add(newItem);
                } else if (!isDirectory(type)) {
                    try {
                        InputStream inputStream = resolver.openInputStream(newUri);
                        if (BitmapFactory.decodeStream(inputStream) != null) {
                            imageUri = newUri.toString();
                        }
                        assert inputStream != null;
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            cursor.close();
        }
        AudioBookResult result = new AudioBookResult();
        result.media = list;
        result.imageUri = imageUri;
        return result;
    }

    private BlockingQueue<String> taskQueue = new ArrayBlockingQueue<>(50);
    private ExecutorService pool = Executors.newFixedThreadPool(10);
    private List<Future<AudioBook>> results = Collections.synchronizedList(new ArrayList<>());

    private List<AudioBook> getList(String initialUri) {
        taskQueue.offer(initialUri);
        while (!isAllDone(results) || !taskQueue.isEmpty()) {
            try {
                String uri = taskQueue.poll();
                if (uri != null) {
                    Future<AudioBook> f = pool.submit(() -> checkDirectory(root, uri));
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
            root = Uri.parse(getInputData().getString(INPUT));
            List<AudioBook> result = getList(DocumentsContract.getTreeDocumentId(root));
            FileOutputStream fos = getApplicationContext().openFileOutput(LIST_OF_DIRS, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(result);
            oos.close();
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.getInstance().logError(e, context);
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

    private AudioBook checkDirectory(Uri root, String id) {
        if (root == null || id == null) {
            return null;
        }
        Uri childUri = DocumentsContract.buildChildDocumentsUriUsingTree(root, id);
        Cursor cursor = resolver.query(childUri, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String childId = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                String type = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
                if (isDirectory(type)) {
                    boolean added = false;
                    while (!added) {
                        added = taskQueue.offer(childId);
                    }
                }
            }
            cursor.close();
        }
        AudioBookResult result = getAudioInDirectory(root, id);
        if (!result.media.isEmpty()) {
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(root, id);
            cursor = resolver.query(docUri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToNext();
                String name = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                cursor.close();
                return new AudioBook(name, id, result.imageUri, result.media, context);
            }
        }
        return null;
    }

    private Uri root;

    public FileScannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }
}

