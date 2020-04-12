package com.example.myfirstapp.defs;

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
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class AudioBook implements Serializable {
    public static int STATUS_IN_PROGRESS = 0;
    public static int STATUS_NOT_BEGUN = 1;
    public static int STATUS_FINISHED = 2;

    public final String rootUri;
    public final List<MediaItem> files;
    public final String displayName;
    private final String imageUri;
    private int positionInTrack;
    private int positionInTrackList;
    private int status;
    private transient Bitmap art;

    public void loadFromFile(Context context) {
        try {
            ObjectInputStream ois = new ObjectInputStream(context.openFileInput(getFileName()));
            AudioBook book = (AudioBook) ois.readObject();
            ois.close();
            this.positionInTrackList = book.positionInTrackList;
            this.positionInTrack = book.positionInTrack;
            this.status = book.status;
            getAlbumArt(context);
        } catch (ClassNotFoundException | InvalidClassException e) {
            context.deleteFile(getFileName());
            Log.d("ASD", "File found for previous version");
        } catch (FileNotFoundException e){
            Log.d("ASD", "No File Found");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus(){
        return status;
    }

    public String getFileName(){
        return displayName.replaceAll("\\W", "");
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

    public int getPositionInTrack(){
        return positionInTrack;
    }

    AudioBook(String name, String rootUri, String imageUri, List<MediaItem> files){
        if (files != null){
            Collections.sort(files);
        }
        this.imageUri = imageUri;
        this.displayName = name;
        this.files = files;
        this.rootUri = rootUri;
        this.status = STATUS_NOT_BEGUN;
    }

    public void setPositionInTrack(int positionInTrack) {
        this.positionInTrack = positionInTrack;
    }

    public void setPositionInTrackList(int positionInTrackList){
        this.positionInTrackList = positionInTrackList;
    }

    public void setFinished(Context context){
        setPositionInTrack(0);
        setPositionInTrackList(0);
        setStatus(AudioBook.STATUS_FINISHED);
        saveConfig(context);
    }

    public void saveConfig(Context context) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(context.openFileOutput(getFileName(), Context.MODE_PRIVATE));
            oos.writeObject(this);
            oos.close();
        } catch (IOException e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }
}
