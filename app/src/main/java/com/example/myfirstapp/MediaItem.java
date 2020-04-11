package com.example.myfirstapp;

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
    String documentUri;
    String displayName;
    private transient MediaMetadataRetriever mmr;
    private transient String title;

    private MediaMetadataRetriever getMMR(Context context){
        if (mmr == null) {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(context, Uri.parse(documentUri));
        }
        return mmr;
    }

    public  String getMetaDataString(Context context) {
        if (documentUri == null) return "";
        MediaMetadataRetriever mmr =  new MediaMetadataRetriever();
        mmr.setDataSource(context, Uri.parse(documentUri));
        List<String> list = Arrays.asList("METADATA_KEY_CD_TRACK_NUMBER", "METADATA_KEY_ALBUM", "METADATA_KEY_ARTIST", "METADATA_KEY_AUTHOR", "METADATA_KEY_COMPOSER", "METADATA_KEY_DATE", "METADATA_KEY_GENRE", "METADATA_KEY_TITLE", "METADATA_KEY_YEAR", "METADATA_KEY_DURATION", "METADATA_KEY_NUM_TRACKS", "METADATA_KEY_WRITER", "METADATA_KEY_MIMETYPE", "METADATA_KEY_ALBUMARTIST", "METADATA_KEY_DISC_NUMBER", "METADATA_KEY_COMPILATION", "METADATA_KEY_HAS_AUDIO", "METADATA_KEY_HAS_VIDEO", "METADATA_KEY_VIDEO_WIDTH", "METADATA_KEY_VIDEO_HEIGHT", "METADATA_KEY_BITRATE", "METADATA_KEY_TIMED_TEXT_LANGUAGES", "METADATA_KEY_IS_DRM", "METADATA_KEY_LOCATION", "METADATA_KEY_VIDEO_ROTATION", "METADATA_KEY_CAPTURE_FRAMERATE", "METADATA_KEY_HAS_IMAGE", "METADATA_KEY_IMAGE_COUNT", "METADATA_KEY_IMAGE_PRIMARY", "METADATA_KEY_IMAGE_WIDTH", "METADATA_KEY_IMAGE_HEIGHT", "METADATA_KEY_IMAGE_ROTATION", "METADATA_KEY_VIDEO_FRAME_COUNT", "METADATA_KEY_EXIF_OFFSET", "METADATA_KEY_EXIF_LENGTH");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            try {
                String meta;
                if ((meta = mmr.extractMetadata(i)) != null) {
                    sb.append(list.get(i)).append(" | ").append(meta).append(System.lineSeparator());
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return sb.toString();
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
