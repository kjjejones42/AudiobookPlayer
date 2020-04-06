package com.example.myfirstapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class AudioBook implements Serializable {
    final String rootUri;
    final List<MediaItem> files;
    final String displayName;
    private final String imageUri;
    private transient Bitmap art;

    public Bitmap getAlbumArt(Context context){
        if (art != null) {
            return art;
        }
        Bitmap result = null;
        try {
            if (imageUri != null) {
                result = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(Uri.parse(imageUri)));
                if (result != null) {
                    art = result;
                    return result;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        byte[] image = null;
        for (MediaItem audioFile : files) {
            mmr.setDataSource(context, Uri.parse(audioFile.documentUri));
            image = mmr.getEmbeddedPicture();
            if (image != null){
                break;
            }
        }
        if (image != null) {
            ByteArrayInputStream bis = new ByteArrayInputStream(image);
            result = BitmapFactory.decodeStream(bis);
        }
        art = result;
        return result;
    }

    AudioBook(String name, String rootUri, String imageUri, List<MediaItem> files){
        if (files != null){
            Collections.sort(files);
        }
        this.imageUri = imageUri;
        this.displayName = name;
        this.files = files;
        this.rootUri = rootUri;
    }

    public void saveConfig(Context context) {
        try {
            Uri uri = DocumentsContract.createDocument(context.getContentResolver(), Uri.parse(rootUri), "text/plain", ".progress");
            ObjectOutputStream oos = new ObjectOutputStream(context.getContentResolver().openOutputStream(uri));
            oos.writeObject(this);
            oos.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return rootUri + " | " + files;
    }
}
