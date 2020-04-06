package com.example.myfirstapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaFormat;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileScannerWorker extends Worker {

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

    private final ContentResolver resolver = getApplicationContext().getContentResolver();

    static final String INPUT = "INPUT";
    static final String LIST_OF_DIRS = "LIST_OF_DIRS";
    private Context context;

    private boolean isAudio(String input){
        return audioFormats.contains(input);
    }
    private boolean isDirectory(String input){
        return input.equals(DocumentsContract.Document.MIME_TYPE_DIR);
    }

    private AudioBookResult getAudioInDirectory(Uri root, String id) {
        List<MediaItem> list = new ArrayList<>();
        String imageUri = null;
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(root, id);
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String childId = cursor.getString(cursor.getColumnIndex("document_id"));
                String type = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
                Uri newUri = DocumentsContract.buildDocumentUriUsingTree(root, childId);
                if (isAudio(type)) {
                    String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                    MediaItem newItem = new MediaItem(newUri.toString(), displayName);
                    list.add(newItem);
                } else if (!isDirectory(type)) {
                    try {
                        if (BitmapFactory.decodeStream(resolver.openInputStream(newUri)) != null){
                            imageUri= newUri.toString();
                        }
                    } catch (Exception ignored) {}
                }
            }
            cursor.close();
        }
        AudioBookResult result = new AudioBookResult();
        result.media = list;
        result.imageUri = imageUri;
        return result;
    }

    private List<AudioBook> recurse(Uri root, String id) {
        List<AudioBook> list = new ArrayList<>();
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(root, id);
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null){
            while (cursor.moveToNext()) {
                String childId = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                String type = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE));
                Uri newUri = DocumentsContract.buildDocumentUriUsingTree(root, childId);
                if (isDirectory(type)) {
                    AudioBookResult result = getAudioInDirectory(root, childId);
                    List<MediaItem> childAudio = result.media;
                    String name = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                    if (!childAudio.isEmpty()) {
                        AudioBook newBook = new AudioBook(name, newUri.toString(), result.imageUri, childAudio);
                        list.add(newBook);
                    }
                    list.addAll(recurse(root, childId));
                }
            }
            cursor.close();
        }
        return list;
    }

    public FileScannerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            final Uri data = Uri.parse(getInputData().getString(INPUT));
            List<AudioBook> result = recurse(data,  DocumentsContract.getTreeDocumentId(data));
            FileOutputStream fos = getApplicationContext().openFileOutput(LIST_OF_DIRS, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(result);
            oos.close();
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }
}

