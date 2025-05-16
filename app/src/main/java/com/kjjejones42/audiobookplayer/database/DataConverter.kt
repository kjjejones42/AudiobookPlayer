package com.kjjejones42.audiobookplayer.database

import androidx.room.TypeConverter
import com.kjjejones42.audiobookplayer.MediaItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64

class DataConverter {
    @TypeConverter
    fun fromMediaItemList(list: List<MediaItem?>?): String? {
        return try {
            if (list == null) return null
            val byteStream = ByteArrayOutputStream()
            ObjectOutputStream(byteStream).use { it.writeObject(list) }
            Base64.getEncoder().encodeToString(byteStream.toByteArray())
        } catch (_: IOException) {
            null
        }
    }

    @TypeConverter
    @Suppress("UNCHECKED_CAST")
    fun toMediaItemList(string: String?): List<MediaItem>? {
        return try {
            if (string == null) return null
            val data = Base64.getDecoder().decode(string)
            ObjectInputStream(ByteArrayInputStream(data)).use {
                it.readObject() as List<MediaItem>
            }
        } catch (_: IOException) {
            null
        } catch (_: ClassNotFoundException) {
            null
        }
    }
}
