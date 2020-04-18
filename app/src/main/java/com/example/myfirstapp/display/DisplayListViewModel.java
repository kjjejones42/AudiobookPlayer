package com.example.myfirstapp.display;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myfirstapp.defs.AudioBook;
import com.example.myfirstapp.defs.FileScannerWorker;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DisplayListViewModel extends ViewModel {

    private MutableLiveData<List<AudioBook>> books = new MutableLiveData<>();

    public DisplayListViewModel() {
    }

    @SuppressWarnings("unchecked")
    void loadFromDisk(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FileScannerWorker.LIST_OF_DIRS);
            ObjectInputStream ois = new ObjectInputStream(fis);
            books.setValue((List<AudioBook>) ois.readObject());
            ois.close();
            for (AudioBook user : Objects.requireNonNull(books.getValue())) {
                user.loadFromFile(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
            context.deleteFile(FileScannerWorker.LIST_OF_DIRS);
            books.setValue(new ArrayList<>());
            saveToDisk(context);
        }
    }

    LiveData<List<AudioBook>> getUsers(Context context) {
        if (books.getValue() == null) {
            loadFromDisk(context);
        }
        return books;
    }

    void saveToDisk(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(FileScannerWorker.LIST_OF_DIRS, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(books.getValue());
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}