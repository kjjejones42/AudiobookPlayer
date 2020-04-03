package com.example.myfirstapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class AudioBook implements Serializable {
    final String rootUri;
    final List<MediaItem> files;
    final String name;
    final String imageUri;

    public Bitmap getAlbumArt(Context context){
        Bitmap result;
        try {
            if (imageUri != null) {
                result = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(Uri.parse(imageUri)));
                if (result != null) {
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
        ByteArrayInputStream bis = new ByteArrayInputStream(image);
        result = BitmapFactory.decodeStream(bis);
        return result;
    }

    AudioBook(String name, String rootUri, String imageUri, List<MediaItem> files){
        if (files != null){
            Collections.sort(files);
        }
        this.imageUri = imageUri;
        this.name = name;
        this.files = files;
        this.rootUri = rootUri;
    }

    @NonNull
    @Override
    public String toString() {
        return rootUri + " | " + files;
    }
}
