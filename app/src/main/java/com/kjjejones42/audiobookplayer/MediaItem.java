package com.kjjejones42.audiobookplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

public class MediaItem implements Serializable, Comparable<MediaItem> {
    public final String filePath;
    public final String uri;
    private final String displayName;

    long getDuration() {
        return duration;
    }

    private final long duration;
    private transient MediaMetadataRetriever mmr;

    @Nullable
    private MediaMetadataRetriever getMMR() {
        try {
            if (mmr == null) {
                mmr = new MediaMetadataRetriever();
                mmr.setDataSource(filePath);
            }
            return mmr;
        } catch (Exception e) {
            return null;
        }
    }

    Bitmap getEmbeddedPicture() {
        Bitmap result = null;
        try {
            mmr = getMMR();
            if (mmr != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(mmr.getEmbeddedPicture());
                result = BitmapFactory.decodeStream(bis);
                bis.close();
            }
        } catch (Exception ignored) {}
        return result;
    }

    public MediaItem(String documentUri, String uri, String displayName, long duration) {
        this.filePath = documentUri;
        this.uri = uri;
        this.displayName = displayName;
        this.duration = duration;
    }

    @NonNull
    @Override
    public String toString() {
        int index = displayName.lastIndexOf('.');
        if (index == -1) {
            return displayName;
        }
        return displayName.substring(0, index);
    }
    @Override
    public int compareTo(MediaItem o) {
        return filePath.compareTo(o.filePath);
    }
}
