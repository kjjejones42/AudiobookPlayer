package com.example.myfirstapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class AudioBook implements Serializable {
    public static final int STATUS_IN_PROGRESS = 0;
    public static final int STATUS_NOT_BEGUN = 1;
    public static final int STATUS_FINISHED = 2;
    private static final long serialVersionUID = 0L;
    private static HashMap<Integer, String> map;
    private static int thumbnailSize;
    public final List<MediaItem> files;
    public final String displayName;
    public final String author;
    private final String directoryPath;
    private final String imagePath;
    public long lastSavedTimestamp;
    private int positionInTrack;
    private int positionInTrackList;
    private int status;
    private transient boolean generatedArt;
    private transient Bitmap thumbnail;
    private transient Bitmap art;
    private transient Palette albumArtPalette;

    public AudioBook(String name, String directoryPath, String imagePath, List<MediaItem> files, String author) {
        if (files != null) {
            Collections.sort(files);
        }
        this.imagePath = imagePath;
        this.displayName = name;
        this.files = files;
        this.directoryPath = directoryPath;
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

    private void generatePalette(Bitmap bitmap) {
        if (bitmap != null) {
            albumArtPalette = Palette.from(bitmap).generate();
        }
    }

    public Palette getAlbumArtPalette() {
        if (albumArtPalette == null) {
            generatePalette(getAlbumArt());
        }
        return albumArtPalette;
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
        if (thumbnail == null) {
            loadThumbnail(activity);
            if (thumbnail == null) {
                generateThumbnailFromAlbumArt(activity, getAlbumArt());
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


    public void loadFromFile(Context context) {
        try {
            InputStream fis = context.openFileInput(getUniqueId());
            ObjectInputStream ois = new ObjectInputStream(fis);
            AudioBook book = (AudioBook) ois.readObject();
            ois.close();
            this.positionInTrackList = book.positionInTrackList;
            this.positionInTrack = book.positionInTrack;
            this.status = book.status;
            this.lastSavedTimestamp = book.lastSavedTimestamp;
            getAlbumArt();
        } catch (IOException | ClassNotFoundException ignored) {
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

    public Bitmap getAlbumArt() {
        if (art != null) {
            return art;
        }
        Bitmap result = null;
        try {
            if (imagePath != null) {
                FileInputStream fis = new FileInputStream(new File(imagePath));
                result = BitmapFactory.decodeStream(fis);
                fis.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
        }
        if (result == null) {
            for (MediaItem file : files) {
                result = file.getEmbeddedPicture();
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

    public String getUniqueId() {
        try {
            return URLEncoder.encode(directoryPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    public int getPositionInTrackList() {
        return positionInTrackList;
    }

    public void setPositionInTrackList(int positionInTrackList) {
        this.positionInTrackList = positionInTrackList;
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

    public long getTotalDuration() {
        long total = 0;
        for (MediaItem item : files) {
            total += item.getDuration();
        }
        return total;
    }

    public void saveConfig(Context context, boolean updateTime) {
        try {
            OutputStream fos = context.openFileOutput(getUniqueId(), Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            if (updateTime) {
                lastSavedTimestamp = new Date().getTime() / 1000L;
            }
            oos.writeObject(this);
            oos.close();
        } catch (IOException e) {
            Utils.logError(e, context);
            e.printStackTrace();
        }
    }

    public void saveConfig(Context context) {
        saveConfig(context, true);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof AudioBook) {
            return this.getUniqueId().equals(((AudioBook) obj).getUniqueId());
        }
        return super.equals(obj);
    }

    @NonNull
    @Override
    public String toString() {
        return displayName + " " + author;
    }
}
