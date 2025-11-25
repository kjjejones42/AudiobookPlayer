package com.kjjejones42.audiobookplayer.ui

import com.kjjejones42.audiobookplayer.database.models.AudioBook

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
    abstract val sortPriority: Int

    abstract fun matchesSearchTerm(value: String): Boolean

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
        override val id = getId(book.uniqueId)
        override val category = book.status
        override val timeStamp = book.lastSavedTimestamp
        override val sortPriority = 1

        override fun toString() = book.displayName

        override fun matchesSearchTerm(value: String): Boolean =
            book.toString().contains(value, ignoreCase = true)
    }

    class Heading internal constructor(override val category: Int) : ListItem() {
        val headingTitle = AudioBook.Status.entries[category].displayName
        override val timeStamp = 0L
        override val sortPriority = 0
        override val id = getId(headingTitle)

        override fun toString() = headingTitle

        override fun matchesSearchTerm(value: String): Boolean = false
    }
}