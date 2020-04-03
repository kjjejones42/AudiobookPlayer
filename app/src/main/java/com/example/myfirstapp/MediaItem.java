package com.example.myfirstapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;

public class MediaItem implements Parcelable, Serializable, Comparable<MediaItem> {
    String documentUri;
    String displayName;


    public Bitmap getAlbumArt(Context context){
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, Uri.parse(documentUri));
        byte[] image = mmr.getEmbeddedPicture();
        if (image == null){
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(image);
        return BitmapFactory.decodeStream(bis);
    }

    MediaItem(String documentUri, String displayName) {
        this.documentUri = documentUri;
        this.displayName = displayName;
    }

    private MediaItem(Parcel in) {
        documentUri = in.readString();
        displayName = in.readString();
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
