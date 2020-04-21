package com.example.myfirstapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

public class MediaItem implements Parcelable, Serializable, Comparable<MediaItem> {
    public String documentUri;
    private String displayName;

    long getDuration() {
        return duration;
    }

    private long duration;
    private transient MediaMetadataRetriever mmr;

    @Nullable
    private MediaMetadataRetriever getMMR(Context context) {
        try {
            if (mmr == null) {
                mmr = new MediaMetadataRetriever();
                mmr.setDataSource(context, Uri.parse(documentUri));
            }
            return mmr;
        } catch (Exception e) {
            return null;
        }
    }

    String extractMetadata(Context context, int keycode) {
        try {
            mmr = getMMR(context);
            if (mmr != null) {
                return mmr.extractMetadata(keycode);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    Bitmap getEmbeddedPicture(Context context) {
        Bitmap result = null;
        try {
            mmr = getMMR(context);
            if (mmr != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(mmr.getEmbeddedPicture());
                result = BitmapFactory.decodeStream(bis);
                bis.close();
            }
        } catch (Exception ignored) {}
        return result;
    }

    public MediaItem(String documentUri, String displayName, long duration) {
        this.documentUri = documentUri;
        this.displayName = displayName;
        this.duration = duration;
    }

    private MediaItem(Parcel in) {
        documentUri = in.readString();
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
        dest.writeString(documentUri);
        dest.writeString(displayName);
    }

    @Override
    public int compareTo(MediaItem o) {
        return displayName.compareTo(o.displayName);
    }
}
