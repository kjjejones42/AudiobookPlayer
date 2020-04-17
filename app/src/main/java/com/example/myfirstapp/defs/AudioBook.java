package com.example.myfirstapp.defs;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.TypedValue;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AudioBook implements Serializable {
    public static int STATUS_IN_PROGRESS = 0;
    public static int STATUS_NOT_BEGUN = 1;
    public static int STATUS_FINISHED = 2;
    private static int thumbnailSize;

    public final String rootUri;
    public final List<MediaItem> files;
    public final String displayName;
    public final String author;
    private final String imageUri;
    private int positionInTrack;
    private int positionInTrackList;
    private int status;
    private transient boolean generatedArt;
    private transient Bitmap thumbnail;
    private transient Bitmap art;

    public static int getThumbnailSize(Activity activity){
        if (thumbnailSize == 0) {
            TypedValue value = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            float ret = value.getDimension(metrics);
            thumbnailSize = (int) Math.round(Math.ceil(ret));
        }
        return thumbnailSize;
    }

    public Bitmap textAsBitmap(String text, float textSize) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(Color.GRAY);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);
        int size = Math.max(width, height);
        Bitmap image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawText(text,
                (size - width) / 2.0f,
                baseline,
                paint);
        generatedArt = true;
        return image;
    }

    public boolean isArtGenerated(){
        return generatedArt;
    }

    AudioBook(String name, String rootUri, String imageUri, List<MediaItem> files, Context context){
        if (files != null){
            Collections.sort(files);
        }
        this.imageUri = imageUri;
        this.displayName = name;
        this.files = files;
        this.rootUri = rootUri;
        this.status = STATUS_NOT_BEGUN;
        List<Integer> keys = Arrays.asList(MediaMetadataRetriever.METADATA_KEY_ARTIST, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, MediaMetadataRetriever.METADATA_KEY_AUTHOR, MediaMetadataRetriever.METADATA_KEY_COMPOSER, MediaMetadataRetriever.METADATA_KEY_WRITER);
        String tempAuthor = null;
        for (MediaItem item : files) {
            MediaMetadataRetriever mmr = item.getMMR(context);
            for (Integer i : keys) {
                tempAuthor = mmr.extractMetadata(i);
                if (tempAuthor != null) {
                    break;
                }
            }
            if (tempAuthor != null) {
                break;
            }
        }
        author = tempAuthor == null ? "" : tempAuthor;
    }


    public Bitmap getThumbnail(Activity activity){
        if (thumbnail == null) {
            int size = getThumbnailSize(activity);
            thumbnail = ThumbnailUtils.extractThumbnail(getAlbumArt(activity), size, size);
        }
        return thumbnail;
    }

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
//            Log.d("ASD", "File found for previous version");
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
        mmr.setDataSource(context, Uri.parse(files.get(0).documentUri));
        byte[]  image = mmr.getEmbeddedPicture();
        if (image != null) {
            ByteArrayInputStream bis = new ByteArrayInputStream(image);
            result = BitmapFactory.decodeStream(bis);
        }
        if (result == null) {
            result = textAsBitmap(displayName.substring(0,1), 600);
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
        return displayName + " "+ author;
    }
}
