package com.example.myfirstapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DisplayListViewModel extends ViewModel {

    private MutableLiveData<List<AudioBook>> users = new MutableLiveData<>();

    @SuppressWarnings("unchecked")
    private void loadFromDisk(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FileScannerWorker.LIST_OF_DIRS);
            ObjectInputStream ois = new ObjectInputStream(fis);
            users.setValue((List<AudioBook>) ois.readObject());
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            users.setValue(new ArrayList<AudioBook>());
            saveToDisk(context);
        }
    }

    LiveData<List<AudioBook>> getUsers(Context context) {
        Log.d("ASD","Load " + users.getValue());
        if (users.getValue() == null) {
            loadFromDisk(context);
        }
        return users;
    }

    void saveToDisk(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(FileScannerWorker.LIST_OF_DIRS, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(users.getValue());
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}