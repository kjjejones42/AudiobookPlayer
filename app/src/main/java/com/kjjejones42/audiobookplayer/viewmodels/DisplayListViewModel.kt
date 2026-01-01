package com.kjjejones42.audiobookplayer.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kjjejones42.audiobookplayer.database.AudiobookRepository
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import com.kjjejones42.audiobookplayer.ui.ListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DisplayListViewModel
    @Inject
    constructor(
        private val audiobookRepository: AudiobookRepository,
    ) : ViewModel() {
        val listItems =
            audiobookRepository.allAndObserve().map { getItemsFromBooks(it) }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        val workUuid = MutableStateFlow<UUID?>(null)

        private val _categorySelectBookId = MutableStateFlow<String?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        val categorySelectBook: StateFlow<AudioBook?> =
            _categorySelectBookId
                .flatMapLatest { id ->
                    if (id == null) {
                        flowOf(null)
                    } else {
                        audiobookRepository.findByNameAndObserve(id)
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null,
                )

        val workNeedsStarting = MutableStateFlow(false)

        fun setCategorySelectBook(bookId: String?) {
            _categorySelectBookId.value = bookId
        }

        fun getWorkStateFlow(context: Context): Flow<WorkInfo?> {
            val uuid = workUuid.value ?: return flowOf(null)
            return WorkManager.getInstance(context).getWorkInfoByIdFlow(uuid)
        }

        fun updateBookStatus(
            book: AudioBook,
            status: AudioBook.Status,
        ) {
            viewModelScope.launch {
                audiobookRepository.updateStatus(book.displayName, status)
            }
        }

        fun getMostRecentBook(): AudioBook? = runBlocking { audiobookRepository.mostRecentBook() }

        companion object {
            fun getItemsFromBooks(books: List<AudioBook>): List<ListItem> {
                val list =
                    books
                        .sortedBy { it.displayName }
                        .map { ListItem.AudioBookContainer(it) }
                        .toMutableList<ListItem>()

                list
                    .toList()
                    .map { it.category }
                    .distinct()
                    .map { ListItem.Heading(it) }
                    .forEach { list.add(it) }

                return list.sortedWith(
                    compareBy(
                        { it.category },
                        { it.sortPriority },
                        { -it.timeStamp },
                    ),
                )
            }
        }
    }
