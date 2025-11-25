package com.kjjejones42.audiobookplayer.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Collections


@Serializable
@Entity
class AudioBook {

    @PrimaryKey
    var displayName: String

    @ColumnInfo
    var baseDir: String? = null

    @ColumnInfo
    var files: List<AudioBookFile>? = null

    @ColumnInfo
    var author: String? = null

    @ColumnInfo
    var imagePath: String? = null

    @ColumnInfo
    var lastSavedTimestamp: Long = 0

    @ColumnInfo
    var positionInTrack: Int = 0

    @ColumnInfo
    var positionInTrackList: Int = 0

    @ColumnInfo
    var status = 0

    constructor() {
        displayName = ""
    }

    constructor(name: String, baseDir: String?, imagePath: String?, files: List<AudioBookFile>?, author: String?) {
        files?.let { Collections.sort(it) }
        this.baseDir = baseDir
        this.imagePath = imagePath
        this.displayName = name
        this.files = files
        this.status = Status.NOT_BEGUN.value
        this.author = author ?: ""
    }

    val totalDuration: Long
        get() {
            return files?.sumOf { it.duration } ?: 0
        }

    fun setStatus(status: Status) {
        this.status = status.value
        when (status) {
            Status.IN_PROGRESS -> {}
            Status.NOT_BEGUN -> {
                lastSavedTimestamp = 0L
                positionInTrackList = 0
                positionInTrack = 0
            }
            Status.FINISHED -> {
                positionInTrackList = 0
                positionInTrack = 0
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is AudioBook) {
            this.uniqueId == other.uniqueId
        } else {
            super.equals(other)
        }
    }

    val uniqueId: String
        get() {
            try {
                return URLEncoder.encode(displayName, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException(e)
            }
        }

    override fun toString(): String {
        return "$displayName $author"
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    enum class Status(val value: Int, val displayName: String) {
        IN_PROGRESS(0, "In Progress"),
        NOT_BEGUN(1, "Not Begun"),
        FINISHED(2, "Finished"),
    }

}