package com.kjjejones42.audiobookplayer;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ThumbnailUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Entity
public class AudioBook {
    public static final int STATUS_IN_PROGRESS = 0;
    public static final int STATUS_NOT_BEGUN = 1;
    public static final int STATUS_FINISHED = 2;
    private static final long serialVersionUID = 0L;
    private static HashMap<Integer, String> map;
    private static int thumbnailSize;
    @PrimaryKey
    @NonNull
    public String displayName;

    @ColumnInfo
    public List<MediaItem> files;

    @ColumnInfo
    public String author;

    @ColumnInfo
    private String imagePath;
    @ColumnInfo
    private long lastSavedTimestamp;
    @ColumnInfo
    private int positionInTrack;
    @ColumnInfo
    private int positionInTrackList;
    @ColumnInfo
    private int status;
    private transient boolean generatedArt;
    private transient Bitmap thumbnail;
    private transient Bitmap art;
    private transient Palette albumArtPalette;
    public AudioBook() {
        displayName = "";

    }

    public AudioBook(@NonNull String name, String imagePath, List<MediaItem> files, String author) {
        if (files != null) {
            Collections.sort(files);
        }
        this.imagePath = imagePath;
        this.displayName = name;
        this.files = files;
        this.status = STATUS_NOT_BEGUN;
        this.author = author == null ? "" : author;
    }

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
             try (TypedArray value = activity.getTheme().obtainStyledAttributes(
                    new int[]{ android.R.attr.listPreferredItemHeight }
            )) {
                 float height = value.getDimension(0, .0F);
                 thumbnailSize = (int) Math.round(Math.ceil(height));
             }
        }
        return thumbnailSize;
    }

    public long getLastSavedTimestamp() {
        return lastSavedTimestamp;
    }

    public void setLastSavedTimestamp(long lastSavedTimestamp) {
        this.lastSavedTimestamp = lastSavedTimestamp;
    }

    public Palette getAlbumArtPalette(Context context) {
        if (albumArtPalette == null) {
            generatePalette(getAlbumArt(context));
        }
        return albumArtPalette;
    }

    private void generatePalette(Bitmap bitmap) {
        if (bitmap != null) {
            albumArtPalette = Palette.from(bitmap).generate();
        }
    }

    public Bitmap getAlbumArt(Context context) {
        if (art != null) {
            return art;
        }
        Bitmap result = null;
        try {
            if (imagePath != null) {
                FileInputStream fis = new FileInputStream(imagePath);
                result = BitmapFactory.decodeStream(fis);
                fis.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
        }
        if (result == null) {
            for (MediaItem file : files) {
                result = file.getEmbeddedPicture(context);
                if (result != null) {
                    break;
                }
            }
        }
        if (result == null) {
            result = getGeneratedAlbumArt(displayName.substring(0, 1));
        }
        art = result;
        generatePalette(art);
        return art;
    }

    private Bitmap getGeneratedAlbumArt(String text) {
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

    public Bitmap getThumbnail(Activity activity) {
        getThumbnailSize(activity);
        if (thumbnail == null) {
            loadThumbnail(activity);
            if (thumbnail == null) {
                generateThumbnailFromAlbumArt(activity, getAlbumArt(activity));
            }
        }
        return thumbnail;
    }

    private File getThumbnailFile(Context context) {
        return new File(context.getCacheDir(), getUniqueId() + ".thumbnail");
    }

    private void generateThumbnailFromAlbumArt(Activity activity, Bitmap bitmap) {
        int size = getThumbnailSize(activity);
        thumbnail = ThumbnailUtils.extractThumbnail(bitmap, size, size);
        if (thumbnail != null) {
            saveThumbnail(activity);
        }
    }

    private void loadThumbnail(Context context) {
        try {
            File file = getThumbnailFile(context);
            FileInputStream fis = new FileInputStream(file);
            thumbnail = BitmapFactory.decodeStream(fis);
            fis.close();
        } catch (FileNotFoundException ignored) {
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

    public int getStatus() {
        return status;
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

    public int getPositionInTrack() {
        return positionInTrack;
    }

    public void setPositionInTrack(int positionInTrack) {
        this.positionInTrack = positionInTrack;
    }

    public long getDurationOfMostRecentTrack() {
        return files.get(getPositionInTrackList()).getDuration();
    }

    public int getPositionInTrackList() {
        return positionInTrackList;
    }

    public void setPositionInTrackList(int positionInTrackList) {
        this.positionInTrackList = positionInTrackList;
    }

    public long getTotalDuration() {
        long total = 0;
        for (MediaItem item : files) {
            total += item.getDuration();
        }
        return total;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof AudioBook) {
            return this.getUniqueId().equals(((AudioBook) obj).getUniqueId());
        }
        return super.equals(obj);
    }

    public String getUniqueId() {
        try {
            return URLEncoder.encode(displayName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return displayName + " " + author;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isGeneratedArt() {
        return generatedArt;
    }

    public void setGeneratedArt(boolean generatedArt) {
        this.generatedArt = generatedArt;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Bitmap getArt() {
        return art;
    }

    public void setArt(Bitmap art) {
        this.art = art;
    }

    public Palette getAlbumArtPalette() {
        return albumArtPalette;
    }

    public void setAlbumArtPalette(Palette albumArtPalette) {
        this.albumArtPalette = albumArtPalette;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    public List<MediaItem> getFiles() {
        return files;
    }

    public void setFiles(List<MediaItem> files) {
        this.files = files;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
