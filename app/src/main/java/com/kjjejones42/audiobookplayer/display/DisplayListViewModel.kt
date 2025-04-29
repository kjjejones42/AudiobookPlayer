package com.kjjejones42.audiobookplayer.display;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kjjejones42.audiobookplayer.AudioBook;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DisplayListViewModel extends ViewModel {

    private final MutableLiveData<List<AudioBook>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ListItem>> listItems = new MutableLiveData<>(new ArrayList<>());

    public DisplayListViewModel() {
        books.observeForever(this::recalculateList);
    }

    @NonNull
    LiveData<List<AudioBook>> getSavedBooks() {
        return books;
    }

    private void recalculateList(List<AudioBook> books) {
        List<ListItem> list = getItemsFromBooks(books);
        listItems.setValue(list);
    }

    private List<ListItem> getItemsFromBooks(List<AudioBook> books) {
        List<ListItem> list = books.stream()
                .sorted(Comparator.comparing(o -> o.displayName))
                .map(ListItem.AudioBookContainer::new)
                .collect(Collectors.toList());
        list.addAll(list.stream()
                .map(ListItem::getCategory)
                .distinct()
                .map(ListItem.Heading::new)
                .collect(Collectors.toList()));
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
    LiveData<List<ListItem>> getListItems() {
        return listItems;
    }

    public void setFilteredListItems(List<AudioBook> items) {
        List<ListItem> filtered = getItemsFromBooks(items);
        listItems.setValue(filtered);
    }

    public void setBooks(List<AudioBook> bookList) {
        books.setValue(bookList);
    }
}