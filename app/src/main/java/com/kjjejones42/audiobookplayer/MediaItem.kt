package com.kjjejones42.audiobookplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.kjjejones42.audiobookplayer.database.DataConverter
import java.io.ByteArrayInputStream
import java.io.Serializable
import java.util.Objects

@kotlinx.serialization.Serializable(with = DataConverter::class)
class MediaItem(uri: Uri, private val displayName: String, val fileName: String, @JvmField val duration: Long) :
Serializable, Comparable<MediaItem?> {

    private var _uri = uri.toString()
    var uri: Uri
        get() = _uri.toUri()
        set(value) {
            _uri = value.toString()
        }

    private fun getMMR(context: Context): MediaMetadataRetriever? {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                val fileDescriptor = it.fileDescriptor
                MediaMetadataRetriever().also {it.setDataSource(fileDescriptor)}
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun getEmbeddedPicture(context: Context): Bitmap? {
        return try {
            getMMR(context)?.use {
                ByteArrayInputStream(it.embeddedPicture).use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (_: Exception) { null }
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
        if (other == null || javaClass != other.javaClass) return false
        return uri == (other as MediaItem).uri
    }

    override fun hashCode(): Int {
        return Objects.hash(uri, displayName, duration)
    }

    override fun compareTo(other: MediaItem?): Int {
        if (other == null) return 0
        return fileName.compareTo(other.fileName)
    }
}
