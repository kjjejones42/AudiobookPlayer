package com.example.myfirstapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AudioBook implements Serializable {
    public final static int STATUS_IN_PROGRESS = 0;
    public final static int STATUS_NOT_BEGUN = 1;
    public final static int STATUS_FINISHED = 2;
    private static int thumbnailSize;
//    private static String TAG = "ASD";

    private final String folderDocumentId;
    public final List<MediaItem> files;
    public final String displayName;
    public final String author;
    private final String imageUri;
    private int positionInTrack;
    private int positionInTrackList;
    private int status;
    public long lastSavedTimestamp;
    private transient boolean generatedArt;
    private transient Bitmap thumbnail;
    private transient Bitmap art;

    private static HashMap<Integer, String> map;

    public static HashMap<Integer, String> getStatusMap() {
        if (map == null) {
            map = new HashMap<>();
            map.put(AudioBook.STATUS_FINISHED, "Finished");
            map.put(AudioBook.STATUS_IN_PROGRESS, "In Progress");
            map.put(AudioBook.STATUS_NOT_BEGUN, "Not Begun");
        }
        return map;
    }

    private static int getThumbnailSize(Activity activity) {
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

    private Bitmap textAsBitmap(String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize((float) 600);
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

    public boolean isArtGenerated() {
        return generatedArt;
    }

    public AudioBook(String name, String folderDocumentId, String imageUri, List<MediaItem> files, Context context) {
        if (files != null) {
            Collections.sort(files);
        }
        this.imageUri = imageUri;
        this.displayName = name;
        this.files = files;
        this.folderDocumentId = folderDocumentId;
        this.status = STATUS_NOT_BEGUN;
        String tempAuthor = null;
        if (files != null) {
            List<Integer> keys = Arrays.asList(MediaMetadataRetriever.METADATA_KEY_ARTIST, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, MediaMetadataRetriever.METADATA_KEY_AUTHOR, MediaMetadataRetriever.METADATA_KEY_COMPOSER, MediaMetadataRetriever.METADATA_KEY_WRITER);
            for (MediaItem item : files) {
                for (Integer i : keys) {
                    tempAuthor = item.extractMetadata(context, i);
                    if (tempAuthor != null) {
                        break;
                    }
                }
                if (tempAuthor != null) {
                    break;
                }
            }
        }
        author = tempAuthor == null ? "" : tempAuthor;
    }


    public Bitmap getThumbnail(Activity activity) {
        if (thumbnail == null) {
            loadThumbnail(activity);
            if (thumbnail == null) {
                int size = getThumbnailSize(activity);
                thumbnail = ThumbnailUtils.extractThumbnail(getAlbumArt(activity), size, size);
            }
            if (thumbnail != null) {
                saveThumbnail(activity);
            }
        }
        return thumbnail;
    }

    private File getThumbnailFile(Context context) {
        return new File(context.getCacheDir(), getUniqueId() + ".thumbnail");
    }

    private void loadThumbnail(Context context) {
        try {
            File file = getThumbnailFile(context);
            FileInputStream fis = new FileInputStream(file);
            thumbnail = BitmapFactory.decodeStream(fis);
            fis.close();
        } catch ( FileNotFoundException ignored){
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveThumbnail(Context context) {
        try {
            File file = getThumbnailFile(context);
            FileOutputStream fos = new FileOutputStream(file);
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void loadFromFile(Context context) {
        try {
            ObjectInputStream ois = new ObjectInputStream(context.openFileInput(getUniqueId()));
            AudioBook book = (AudioBook) ois.readObject();
            ois.close();
            this.positionInTrackList = book.positionInTrackList;
            this.positionInTrack = book.positionInTrack;
            this.status = book.status;
            this.lastSavedTimestamp = book.lastSavedTimestamp;
            getAlbumArt(context);
        } catch (ClassNotFoundException | InvalidClassException e) {
            context.deleteFile(getUniqueId());
//            Log.d(TAG, "File found for previous version");
        } catch (FileNotFoundException e) {
//            Log.d(TAG, "File not found for " + displayName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStatus(int status) {
        switch (status) {
            case AudioBook.STATUS_IN_PROGRESS:
                break;
            case AudioBook.STATUS_NOT_BEGUN:
                lastSavedTimestamp = 0L;
            case AudioBook.STATUS_FINISHED:
                setPositionInTrackList(0);
                setPositionInTrack(0);
                break;
        }
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public Bitmap getAlbumArt(Context context) {
        if (art != null) {
            return art;
        }
        Bitmap result = null;
        try {
            if (imageUri != null) {
                InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(imageUri));
                result = BitmapFactory.decodeStream(inputStream);
                assert inputStream != null;
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception ignored){}
        if (result == null) {
            for (MediaItem file : files) {
                result = file.getEmbeddedPicture(context);
                if (result != null) {
                    break;
                }
            }
        }
        if (result == null) {
            result = textAsBitmap(displayName.substring(0, 1));
        }
        art = result;
        return art;
    }

    public String getUniqueId(){
        return folderDocumentId.replaceAll("\\W", "");
    }


    public int getPositionInTrackList() {
        return positionInTrackList;
    }

    public int getPositionInTrack() {
        return positionInTrack;
    }


    public void setPositionInTrack(int positionInTrack) {
        this.positionInTrack = positionInTrack;
    }

    public void setPositionInTrackList(int positionInTrackList) {
        this.positionInTrackList = positionInTrackList;
    }

    public void setFinished(Context context) {
        setPositionInTrack(0);
        setPositionInTrackList(0);
        setStatus(AudioBook.STATUS_FINISHED);
        saveConfig(context);
    }

    public long getDurationOfMostRecentTrack(){
       return files.get(getPositionInTrackList()).getDuration();
    }

    public long getTotalDuration(){
        long total = 0;
        for (MediaItem item : files) {
            total += item.getDuration();
        }
        return total;
    }


    public void saveConfig(Context context) {
        try {
            lastSavedTimestamp = new Date().getTime() / 1000L;
            ObjectOutputStream oos = new ObjectOutputStream(context.openFileOutput(getUniqueId(), Context.MODE_PRIVATE));
            oos.writeObject(this);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return displayName + " " + author;
    }
}
