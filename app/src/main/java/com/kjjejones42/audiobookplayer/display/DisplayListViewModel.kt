package com.kjjejones42.audiobookplayer.display

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.display.ListItem.AudioBookContainer
import com.kjjejones42.audiobookplayer.display.ListItem.Heading

class DisplayListViewModel : ViewModel() {
    private val _savedBooks = MutableLiveData<List<AudioBook>>(ArrayList())
    val savedBooks: LiveData<List<AudioBook>>
        get() = _savedBooks

    private val _listItems = MutableLiveData<List<ListItem>>(ArrayList())
    val listItems: LiveData<List<ListItem>>
        get() = _listItems

    init {
        _savedBooks.observeForever { setFilteredListItems(it) }
    }

    private fun getItemsFromBooks(books: List<AudioBook>): List<ListItem> {
        val list = books
            .sortedBy { it.displayName }
            .map { AudioBookContainer(it) }
            .toMutableList<ListItem>()

        list.toList()
            .map { it.category }
            .distinct()
            .map { Heading(it) }
            .forEach{ list.add(it) }

        return list.sortedWith(compareBy( {it.category}, {it.type.value}, {-it.timeStamp} ))
    }

    fun setFilteredListItems(items: List<AudioBook>) {
        _listItems.value = getItemsFromBooks(items)
    }

    fun setBooks(bookList: List<AudioBook>) {
        _savedBooks.value = bookList
    }
}