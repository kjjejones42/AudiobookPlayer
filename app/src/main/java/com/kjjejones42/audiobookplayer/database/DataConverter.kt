package com.kjjejones42.audiobookplayer.database

import androidx.room.TypeConverter
import com.kjjejones42.audiobookplayer.MediaItem
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64

class DataConverter : KSerializer<MediaItem> {
    override val descriptor: SerialDescriptor = SerialDescriptor("MediaItem", String.serializer().descriptor)
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

    override fun serialize(encoder: Encoder, value: MediaItem) {
        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { it.writeObject(value) }
        val string = Base64.getEncoder().encodeToString(byteStream.toByteArray())
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): MediaItem {
        val string = decoder.decodeString()
        return try {
            val data = Base64.getDecoder().decode(string)
            ObjectInputStream(ByteArrayInputStream(data)).use {
                it.readObject() as MediaItem
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
