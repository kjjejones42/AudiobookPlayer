package com.kjjejones42.audiobookplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.Serializable
import java.util.Objects

class MediaItem(documentUri: Uri, private val displayName: String, @JvmField val duration: Long) :
    Serializable, Comparable<MediaItem?> {
    private val uri = documentUri.toString()

    @Transient
    private var mmr: MediaMetadataRetriever? = null

    private fun getMMR(context: Context): MediaMetadataRetriever? {
        if (mmr == null) {
            try {
                context.contentResolver.openAssetFileDescriptor(getUri(), "r")
                    .use { assetFileDescriptor ->
                        if (assetFileDescriptor != null) {
                            val fileDescriptor = assetFileDescriptor.fileDescriptor
                            mmr = MediaMetadataRetriever()
                            mmr!!.setDataSource(fileDescriptor)
                        }
                    }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
        return mmr
    }

    fun getEmbeddedPicture(context: Context): Bitmap? {
        var result: Bitmap? = null
        try {
            mmr = getMMR(context)
            if (mmr != null) {
                val bis = ByteArrayInputStream(mmr!!.embeddedPicture)
                result = BitmapFactory.decodeStream(bis)
                bis.close()
                mmr!!.close()
            }
        } catch (ignored: Exception) {
        }
        return result
    }

    fun getUri(): Uri {
        return uri.toUri()
    }


    override fun toString(): String {
        val index = displayName.lastIndexOf('.')
        if (index == -1) {
            return displayName
        }
        return displayName.substring(0, index)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        return uri == (other as MediaItem).uri
    }

    override fun hashCode(): Int {
        return Objects.hash(uri, displayName, duration, mmr)
    }

    override fun compareTo(other: MediaItem?): Int {
        if (other == null) return 0
        return displayName.compareTo(other.displayName)
    }
}
