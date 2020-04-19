package com.example.myfirstapp.player;

import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlayerViewModel extends ViewModel {

    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();
    private MutableLiveData<Long> position = new MutableLiveData<>();
    private MutableLiveData<MediaMetadataCompat> metadata = new MutableLiveData<>();
    static private MediaMetadataCompat emptyMetadata;

    public PlayerViewModel(){
        isPlaying.setValue(true);
        clear();
    }

    private MediaMetadataCompat getEmptyMetadata(){
        if (emptyMetadata == null){
            emptyMetadata = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "")
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 100)
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 0)
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                            .build();
        }
        return emptyMetadata;
    }

    void clear() {
        position.setValue(0L);
        metadata.setValue(getEmptyMetadata());
    }

    @NonNull
    LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    void setIsPlaying(boolean isPlaying) {
        Boolean b = this.isPlaying.getValue();
        if (b != null && b != isPlaying) {
            this.isPlaying.setValue(isPlaying);
        }
    }

    @NonNull
    LiveData<Long> getPosition() {
        return position;
    }

    void setPosition(long position) {
        Long l = this.position.getValue();
        if (l != null && l != position) {
            this.position.setValue(position);
        }
    }

    @NonNull
    LiveData<MediaMetadataCompat> getMetadata() {
        return metadata;
    }

    void setMetadata(MediaMetadataCompat metadata) {
        this.metadata.setValue(metadata);
    }

}
