package com.kjjejones42.audiobookplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.Serializable;

public class MediaItem implements Serializable, Comparable<MediaItem> {

    private final String uri;
    private final String displayName;
    private final long duration;
    private transient MediaMetadataRetriever mmr;

    @Nullable
    private MediaMetadataRetriever getMMR(Context context) {
        try {
            if (mmr == null) {
                AssetFileDescriptor assetFileDescriptor = context.getContentResolver().openAssetFileDescriptor(getUri(), "r");
                if (assetFileDescriptor != null) {
                    FileDescriptor fileDescriptor = assetFileDescriptor.getFileDescriptor();
                    mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(fileDescriptor);
                }
            }
            return mmr;
        } catch (Exception e) {
            return null;
        }
    }

    public Bitmap getEmbeddedPicture(Context context) {
        Bitmap result = null;
        try {
            mmr = getMMR(context);
            if (mmr != null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(mmr.getEmbeddedPicture());
                result = BitmapFactory.decodeStream(bis);
                bis.close();
                mmr.close();
            }
        } catch (Exception ignored) {}
        return result;
    }

    public MediaItem(Uri documentUri, String displayName, long duration) {
        this.uri = documentUri.toString();
        this.displayName = displayName;
        this.duration = duration;
    }

    public Uri getUri() {
        return Uri.parse(uri);
    }
    long getDuration() {
        return duration;
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
        return uri.compareTo(o.uri);
    }
}
