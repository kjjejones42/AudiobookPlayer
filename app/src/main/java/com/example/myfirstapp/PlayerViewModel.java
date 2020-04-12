package com.example.myfirstapp;

import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlayerViewModel extends ViewModel {

    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(true);
    private MutableLiveData<Long> position = new MutableLiveData<>(0L);
    private MutableLiveData<MediaMetadataCompat> metadata = new MutableLiveData<>(new MediaMetadataCompat.Builder().build());

    public PlayerViewModel(){
        Log.d("ASD", "PlayerViewModel: ");
    }

    public void clear(){
        metadata.setValue(new MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 100)
                .build());
        position.setValue(0L);
    }

    public LiveData<Boolean> getIsPlaying(){
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying){
        if (this.isPlaying.getValue() != isPlaying){
            Log.d("ASD", "setIsPlaying: " + isPlaying);
            this.isPlaying.setValue(isPlaying);
        }
    }

    public LiveData<Long> getPosition() {
        return position;
    }

    public void setPosition(long position) {
        if (this.position.getValue() != position) {
            this.position.setValue(position);
        }
    }

    public LiveData<MediaMetadataCompat> getMetadata() {
        return metadata;
    }

    public void setMetadata(MediaMetadataCompat metadata) {
        this.metadata.setValue(metadata);
    }

}
