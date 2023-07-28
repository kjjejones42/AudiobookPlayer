package com.kjjejones42.audiobookplayer.display;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.Utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DisplayListViewModel extends ViewModel {

    private final MutableLiveData<List<AudioBook>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ListItem>> listItems = new MutableLiveData<>(new ArrayList<>());

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
            listItems.setValue(listItems.getValue());
        } catch (FileNotFoundException | ClassNotFoundException | InvalidClassException ignored) {
            onLoadError(context);
        } catch (Exception e) {
            Utils.logError(e, context);
            e.printStackTrace();
            onLoadError(context);
        }
    }

    private void onLoadError(Context context) {
        context.deleteFile(FileScannerWorker.LIST_OF_DIRS);
        books.setValue(new ArrayList<>());
        saveToDisk(context);
    }

    @NonNull
    LiveData<List<AudioBook>> getSavedBooks(Context context) {
        if (books.getValue() == null) {
            loadFromDisk(context);
        }
        return books;
    }

    @NonNull
    LiveData<List<ListItem>> getListItems(Context context) {
        if (listItems.getValue() == null) {
            loadFromDisk(context);
        }
        return listItems;
    }

    void setListItems(List<ListItem> newList) {
        listItems.setValue(newList);
    }

    void saveToDisk(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(FileScannerWorker.LIST_OF_DIRS, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(books.getValue());
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Utils.logError(e, context);
        }
    }
}