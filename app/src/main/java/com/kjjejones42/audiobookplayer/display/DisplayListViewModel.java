package com.kjjejones42.audiobookplayer.display;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kjjejones42.audiobookplayer.AudioBook;
import com.kjjejones42.audiobookplayer.Utils;
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase;

import java.util.ArrayList;
import java.util.List;

public class DisplayListViewModel extends ViewModel {

    private final MutableLiveData<List<AudioBook>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ListItem>> listItems = new MutableLiveData<>(new ArrayList<>());

    public DisplayListViewModel() {
    }

    void loadFromDisk(Context context) {
        try {
            books.setValue(AudiobookDatabase.getInstance(context).audiobookDao().getAll());
            listItems.setValue(listItems.getValue());
        } catch (Exception e) {
            Utils.logError(e, context);
            e.printStackTrace();
        }
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
}