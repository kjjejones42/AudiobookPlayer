package com.kjjejones42.audiobookplayer.display

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.display.ListItem.AudioBookContainer
import com.kjjejones42.audiobookplayer.display.ListItem.Heading
import java.util.stream.Collectors

class DisplayListViewModel : ViewModel() {
    private val books = MutableLiveData<List<AudioBook>>(ArrayList())
    val listItems = MutableLiveData<List<ListItem>>(ArrayList())

    init {
        books.observeForever { books -> this.recalculateList(books) }
    }

    val savedBooks: LiveData<List<AudioBook>>
        get() = books

    private fun recalculateList(books: List<AudioBook>) {
        val list = getItemsFromBooks(books)
        listItems.value = list
    }

    private fun getItemsFromBooks(books: List<AudioBook>): List<ListItem> {
        val list: MutableList<ListItem> = books.stream()
            .sorted(Comparator.comparing { o -> o.displayName })
            .filter { book -> book != null}
            .map { book -> AudioBookContainer(book) }
            .collect(Collectors.toList())

        list.toList().stream()
            .map { obj -> obj.category }
            .distinct()
            .map { category -> Heading(category) }
            .forEach{item -> list.add(item)}

        list.sortWith { o1, o2 ->
            val i = o1.category - o2.category
            if (i == 0) {
                val j = o1.headingOrItem - o2.headingOrItem
                if (j == 0) {
                    return@sortWith (o2.timeStamp - o1.timeStamp).toInt()
                }
                return@sortWith j
            }
            i
        }
        return list
    }

    fun getListItems(): LiveData<List<ListItem>> {
        return listItems
    }

    fun setFilteredListItems(items: List<AudioBook>) {
        val filtered = getItemsFromBooks(items)
        listItems.value = filtered
    }

    fun setBooks(bookList: List<AudioBook>) {
        books.value = bookList
    }
}