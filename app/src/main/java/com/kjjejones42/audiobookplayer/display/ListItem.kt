package com.kjjejones42.audiobookplayer.display

import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.AudioBookStatus

enum class ListItemType(val value: Int) {
    HEADING(0),
    ITEM(1)
}

abstract class ListItem {

    companion object {
        private val idMap: MutableMap<String?, Long> = HashMap()

        fun getId(name: String?): Long {
            return idMap.computeIfAbsent(name) { idMap.entries.size.toLong() }
        }
    }

    abstract val id: Long
    abstract val category: Int
    abstract val timeStamp: Long
    abstract val type: ListItemType

    override fun equals(other: Any?): Boolean {
        if (other is ListItem) {
            return other.toString() == this.toString()
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    class AudioBookContainer internal constructor(val book: AudioBook) : ListItem() {
        override val id: Long = getId(book.uniqueId)
        override val category: Int = book.status
        override val timeStamp: Long = book.lastSavedTimestamp
        override val type: ListItemType = ListItemType.ITEM

        override fun toString(): String {
            return book.displayName
        }
    }

    class Heading internal constructor(override val category: Int) : ListItem() {
        val headingTitle: String? = AudioBookStatus.entries[category].displayName
        override val timeStamp: Long = 0
        override val type: ListItemType = ListItemType.HEADING
        override val id: Long = getId(headingTitle)

        override fun toString(): String {
            return headingTitle ?: ""
        }
    }
}
