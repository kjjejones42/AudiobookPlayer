package com.example.myfirstapp.player;

import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

@SuppressWarnings("WeakerAccess")
public class PlayerViewModel extends ViewModel {

    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(true);
    private MutableLiveData<Long> position = new MutableLiveData<>(0L);
    private MutableLiveData<MediaMetadataCompat> metadata = new MutableLiveData<>(new MediaMetadataCompat.Builder().build());

    void clear(){
        metadata.setValue(new MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 100)
                .build());
        position.setValue(0L);
    }

    @NonNull
    LiveData<Boolean> getIsPlaying(){
        return isPlaying;
    }

    void setIsPlaying(boolean isPlaying){
        Boolean b = this.isPlaying.getValue();
        if (b != null && b != isPlaying){
            this.isPlaying.setValue(isPlaying);
        }
    }

    @NonNull
    LiveData<Long> getPosition() {
        return position;
    }

    void setPosition(long position) {
        Long l = this.position.getValue();
        if (l != null  && l != position) {
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
