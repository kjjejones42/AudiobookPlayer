package com.kjjejones42.audiobookplayer.display

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.database.AudiobookRepository
import com.kjjejones42.audiobookplayer.display.ListItem.AudioBookContainer
import com.kjjejones42.audiobookplayer.display.ListItem.Heading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DisplayListViewModel @Inject constructor(
    audiobookRepository: AudiobookRepository
) : ViewModel() {

    val listItems = audiobookRepository
        .allAndObserve()
        .map { getItemsFromBooks(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val workUuid = MutableStateFlow<UUID?>(null)

    val workNeedsStarting = MutableStateFlow(false)

    fun getWorkStateFlow(context: Context): Flow<WorkInfo?> {
        val uuid = workUuid.value ?: return flowOf(null)
        return WorkManager.getInstance(context).getWorkInfoByIdFlow(uuid)
    }

    companion object {
        fun getItemsFromBooks(books: List<AudioBook>): List<ListItem> {
            val list = books
                .sortedBy { it.displayName }
                .map { AudioBookContainer(it) }
                .toMutableList<ListItem>()

            list.toList()
                .map { it.category }
                .distinct()
                .map { Heading(it) }
                .forEach { list.add(it) }

            return list.sortedWith(compareBy({ it.category }, { it.type.value }, { -it.timeStamp }))
        }
    }
}