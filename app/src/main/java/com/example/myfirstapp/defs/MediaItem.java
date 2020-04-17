package com.example.myfirstapp.defs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class MediaItem implements Parcelable, Serializable, Comparable<MediaItem> {
    public String documentUri;
    public String displayName;
    private transient MediaMetadataRetriever mmr;
    private transient String title;

    MediaMetadataRetriever getMMR(Context context){
        if (mmr == null) {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(context, Uri.parse(documentUri));
        }
        return mmr;
    }

    public Bitmap getAlbumArt(Context context){
        byte[] image = getMMR(context).getEmbeddedPicture();
        if (image == null){
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(image);
        return BitmapFactory.decodeStream(bis);
    }

    public void generateTitle(Context context) {
        if (title == null) {
            title = getMMR(context).extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        }
    }


    MediaItem(String documentUri, String displayName) {
        this.documentUri = documentUri;
        this.displayName = displayName;
    }

    private MediaItem(Parcel in) {
        documentUri = in.readString();
        displayName = in.readString();
    }

    @NonNull
    @Override
    public String toString() {
        return (title == null) ? displayName.substring(0, displayName.lastIndexOf('.')) : title;
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
