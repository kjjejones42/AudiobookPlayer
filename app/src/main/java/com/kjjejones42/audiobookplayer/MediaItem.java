package com.kjjejones42.audiobookplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

public class MediaItem implements Parcelable, Serializable, Comparable<MediaItem> {
    public final String filePath;
    public final String uri;
    private final String displayName;

    long getDuration() {
        return duration;
    }

    private long duration;
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

    private MediaItem(Parcel in) {
        filePath = in.readString();
        uri = in.readString();
        displayName = in.readString();
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

    public static final Creator<MediaItem> CREATOR = new Creator<MediaItem>() {
        @Override
        public MediaItem createFromParcel(Parcel in) {
            return new MediaItem(in);
        }

        @Override
        public MediaItem[] newArray(int size) {
            return new MediaItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filePath);
        dest.writeString(uri);
        dest.writeString(displayName);
    }

    @Override
    public int compareTo(MediaItem o) {
        return filePath.compareTo(o.filePath);
    }
}
