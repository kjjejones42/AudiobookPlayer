package com.kjjejones42.audiobookplayer.database.models

import android.net.Uri
import androidx.core.net.toUri
import com.kjjejones42.audiobookplayer.database.DataConverter
import kotlinx.serialization.Serializable
import java.util.Objects

@Serializable(with = DataConverter::class)
class AudioBookFile(uri: Uri, private val displayName: String, val fileName: String, @JvmField val duration: Long) :
java.io.Serializable, Comparable<AudioBookFile?> {
    private var _uri = uri.toString()
    var uri: Uri
        get() = _uri.toUri()
        set(value) {
            _uri = value.toString()
        }

    override fun toString(): String {
        val index = displayName.lastIndexOf('.')
        if (index == -1) {
            return displayName
        }
        return displayName.take(index)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioBookFile) return false
        return uri == other.uri
    }

    override fun hashCode(): Int {
        return Objects.hash(uri, displayName, duration)
    }

    override fun compareTo(other: AudioBookFile?): Int {
        if (other == null) return 0
        return fileName.compareTo(other.fileName)
    }

    companion object {
        private const val serialVersionUID: Long = 1234567L
    }
}