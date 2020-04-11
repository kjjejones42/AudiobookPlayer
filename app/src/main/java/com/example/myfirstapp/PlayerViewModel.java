package com.example.myfirstapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PlayerViewModel extends ViewModel {
    private MutableLiveData<Integer> duration;
    private MutableLiveData<Integer> position;

    LiveData<Integer> getDuration(){
        return duration;
    }
}
