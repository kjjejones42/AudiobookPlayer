package com.example.myfirstapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class AudioBook implements Serializable {
    final String rootUri;
    final List<MediaItem> files;
    final String displayName;
    private final String imageUri;
    private int positionInTrackList;
    private long durationOfTrack;
    private transient Bitmap art;

    public void loadFromFile(Context context) {
        try {
            ObjectInputStream ois = new ObjectInputStream(context.openFileInput(displayName));
            AudioBook book = (AudioBook) ois.readObject();
            ois.close();
            this.positionInTrackList = book.positionInTrackList;
            this.durationOfTrack = book.durationOfTrack;
        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

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


    public int getPositionInTrackList(){
        return positionInTrackList;
    }

    public long getDurationOfTrack(){
        return durationOfTrack;
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

    public void saveConfig(Context context, int positionInTrackList, long durationOfTrack) {
        try {
            this.positionInTrackList = positionInTrackList;
            this.durationOfTrack = durationOfTrack;
            ObjectOutputStream oos = new ObjectOutputStream(context.openFileOutput(displayName, Context.MODE_PRIVATE));
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
