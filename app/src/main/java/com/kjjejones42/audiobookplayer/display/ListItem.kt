package com.kjjejones42.audiobookplayer.display

import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.AudioBook.Companion.statusMap

abstract class ListItem {
    abstract val id: Long
    abstract val category: Int
    abstract val timeStamp: Long
    abstract val headingOrItem: Int

    override fun equals(obj: Any?): Boolean {
        if (obj is ListItem) {
            return obj.toString() == this.toString()
        }
        return super.equals(obj)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    class AudioBookContainer internal constructor(val book: AudioBook) : ListItem() {
        override val id: Long = getId(book.uniqueId)

        override val category: Int = book.getStatus()

        override val timeStamp: Long = book.lastSavedTimestamp

        override val headingOrItem: Int = TYPE_ITEM
    }

    class Heading internal constructor(override val category: Int) : ListItem() {
        val headingTitle: String? = statusMap!![category]
        override val timeStamp: Long = 0
        override val headingOrItem: Int = TYPE_HEADING
        override val id: Long = getId(headingTitle)

        override fun toString(): String {
            return headingTitle!!
        }
    }

    companion object {
        const val TYPE_HEADING: Int = 0
        const val TYPE_ITEM: Int = 1

        private val idMap: MutableMap<String?, Long> = HashMap()

        fun getId(name: String?): Long {
            val id: Long
            if (idMap.containsKey(name)) {
                val value =
                    idMap[name]
                        ?: throw RuntimeException()
                id = value
            } else {
                id = idMap.entries.size.toLong()
                idMap[name] = id
            }
            return id
        }
    }
}
