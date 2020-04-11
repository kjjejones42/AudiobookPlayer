package com.example.myfirstapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlayerViewModel extends ViewModel {
    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private MutableLiveData<Long> position = new MutableLiveData<>(0L);

    public LiveData<Boolean> getIsPlaying(){
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying){
        if (this.isPlaying.getValue() != isPlaying){
            this.isPlaying.setValue(isPlaying);
        }
    }

    public LiveData<Long> getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        if (this.duration.getValue() != duration) {
            this.duration.setValue(duration);
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
}
