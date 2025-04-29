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
        try {
            if (list == null) return null
            val baos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(baos)
            oos.writeObject(list)
            oos.close()
            return Base64.getEncoder().encodeToString(baos.toByteArray())
        } catch (ignored: IOException) {
            return null
        }
    }

    @TypeConverter
    @Suppress("UNCHECKED_CAST")
    fun toMediaItemList(string: String?): List<MediaItem>? {
        try {
            if (string == null) return null
            val data = Base64.getDecoder().decode(string)
            val ois = ObjectInputStream(ByteArrayInputStream(data))
            val o = ois.readObject()
            ois.close()
            return o as List<MediaItem>
        } catch (ignored: IOException) {
            return null
        } catch (ignored: ClassNotFoundException) {
            return null
        }
    }
}
