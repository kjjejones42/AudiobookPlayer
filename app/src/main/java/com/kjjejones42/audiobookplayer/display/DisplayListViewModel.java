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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class DisplayListViewModel extends ViewModel {

    private final MutableLiveData<List<AudioBook>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ListItem>> listItems = new MutableLiveData<>(new ArrayList<>());

    public DisplayListViewModel() {
    }

    @NonNull
    LiveData<List<AudioBook>> getSavedBooks(Context context) {
        if (books.getValue() == null) {
            loadFromDatabase(context);
        }
        return books;
    }

    void loadFromDatabase(Context context) {
        try {
            books.observeForever(this::recalculateList);
            books.setValue(AudiobookDatabase.getInstance(context).audiobookDao().getAll());
            listItems.setValue(listItems.getValue());
        } catch (Exception e) {
            Utils.logError(e, context);
            e.printStackTrace();
        }
    }

    private void recalculateList(List<AudioBook> books) {
        List<ListItem> list = getItemsFromBooks(books);
        listItems.setValue(list);
    }

    private List<ListItem> getItemsFromBooks(List<AudioBook> books) {
        List<ListItem> list = new ArrayList<>();
        books.sort(Comparator.comparing(o -> o.displayName));
        for (AudioBook book : books) {
            list.add(new ListItem.AudioBookContainer(book));
        }
        List<Integer> letters = new ArrayList<>();
        for (ListItem item : list) {
            letters.add(item.getCategory());
        }
        letters = new ArrayList<>(new HashSet<>(letters));
        for (Integer letter : letters) {
            list.add(new ListItem.Heading(letter));
        }
        list.sort((o1, o2) -> {
            int i = o1.getCategory() - o2.getCategory();
            if (i == 0) {
                int j = o1.getHeadingOrItem() - o2.getHeadingOrItem();
                if (j == 0) {
                    return (int) (o2.getTimeStamp() - o1.getTimeStamp());
                }
                return j;
            }
            return i;
        });
        return list;
    }

    @NonNull
    LiveData<List<ListItem>> getListItems(Context context) {
        if (listItems.getValue() == null) {
            loadFromDatabase(context);
        }
        return listItems;
    }

    public void setFilteredListItems(List<AudioBook> items) {
        List<ListItem> filtered = getItemsFromBooks(items);
        listItems.setValue(filtered);
    }
}