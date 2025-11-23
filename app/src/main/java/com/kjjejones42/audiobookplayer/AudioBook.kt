package com.kjjejones42.audiobookplayer

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ThumbnailUtils
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt


enum class AudioBookStatus(val value: Int, val displayName: String) {
    IN_PROGRESS(0, "In Progress"),
    NOT_BEGUN(1, "Not Begun"),
    FINISHED(2, "Finished"),
}

@Serializable
@Entity
class AudioBook {

    companion object {
        private var thumbnailSize = 0

        private fun getThumbnailSize(activity: Activity): Int {
            if (thumbnailSize == 0) {
                activity.theme.obtainStyledAttributes(
                    intArrayOf(android.R.attr.listPreferredItemHeight)
                ).use { thumbnailSize = ceil(it.getDimension(0, .0f).toDouble()).roundToInt() }
            }
            return thumbnailSize
        }
    }

    @PrimaryKey
    var displayName: String

    @ColumnInfo
    var baseDir: String? = null

    @ColumnInfo
    var files: List<MediaItem>? = null

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

    @Transient
    @kotlinx.serialization.Transient
    var isArtGenerated: Boolean = false
        private set

    @Transient
    @kotlinx.serialization.Transient
    private var thumbnail: Bitmap? = null

    @Transient
    @kotlinx.serialization.Transient
    private var art: Bitmap? = null

    @Transient
    @kotlinx.serialization.Transient
    private var albumArtPalette: Palette? = null

    constructor() {
        displayName = ""
    }

    constructor(name: String, baseDir: String?, imagePath: String?, files: List<MediaItem>?, author: String?) {
        files?.let { Collections.sort(it) }
        this.baseDir = baseDir
        this.imagePath = imagePath
        this.displayName = name
        this.files = files
        this.status = AudioBookStatus.NOT_BEGUN.value
        this.author = author ?: ""
    }

    fun getAlbumArtPalette(context: Context): Palette? {
        if (albumArtPalette == null) {
            generatePalette(getAlbumArt(context))
        }
        return albumArtPalette
    }

    private fun generatePalette(bitmap: Bitmap?) {
        bitmap?.let { albumArtPalette = Palette.from(it).generate() }
    }

    fun getAlbumArtInit(context: Context): Bitmap? {
        art?.let { return it }
        return try {
            imagePath?.takeIf { it.isNotEmpty() }?.let {
                FileInputStream(it).use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (e: FileNotFoundException) {
            logError(e, "Couldn't get album art", context)
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getAlbumArt(context: Context): Bitmap {
        art?.let { return it }
        val result = try {
            imagePath?.takeIf { it.isNotEmpty() }?.let {
                FileInputStream(it).use {
                    BitmapFactory.decodeStream(it)
                }
            }
        } catch (e: FileNotFoundException) {
            logError(e, "Couldn't get album art", context)
            null
        } catch (_: Exception) {
            null
        }
        ?: files?.firstNotNullOfOrNull { it.getEmbeddedPicture(context) }
        ?: getGeneratedAlbumArt(displayName.first().toString())

        art = result
        generatePalette(result)
        return result
    }

    private fun getGeneratedAlbumArt(text: String): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 600f
        paint.color = Color.GRAY
        paint.textAlign = Paint.Align.LEFT
        val baseline = -paint.ascent()
        val width = (paint.measureText(text) + 0.5f).toInt()
        val height = (baseline + paint.descent() + 0.5f).toInt()
        val size = max(width.toDouble(), height.toDouble()).toInt()
        val image = createBitmap(size, size)
        val canvas = Canvas(image)
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawText(
            text,
            (size - width) / 2.0f,
            baseline,
            paint
        )
        isArtGenerated = true
        return image
    }

    fun getThumbnail(activity: Activity): Bitmap? {
        getThumbnailSize(activity)
        return thumbnail
            ?: loadThumbnail(activity)
            ?: generateThumbnailFromAlbumArt(activity, getAlbumArt(activity))
            .also { thumbnail = it }
    }

    private fun getThumbnailFile(context: Context): File {
        return File(context.cacheDir, "$uniqueId.thumbnail")
    }

    private fun generateThumbnailFromAlbumArt(activity: Activity, bitmap: Bitmap): Bitmap {
        val size = getThumbnailSize(activity)
        val thumbnail = ThumbnailUtils.extractThumbnail(bitmap, size, size)
        saveThumbnailToFile(activity, thumbnail)
        return thumbnail
    }

    private fun loadThumbnail(context: Context): Bitmap? {
        try {
            val file = getThumbnailFile(context)
            FileInputStream(file).use {
                return BitmapFactory.decodeStream(it)
            }
        } catch (_: FileNotFoundException) {
        } catch (e: Exception) {
            logError(e, "Couldn't load thumbnail", context)
        }
        return null
    }

    private fun saveThumbnailToFile(context: Context, thumbnail: Bitmap) {
        try {
            val file = getThumbnailFile(context)
            FileOutputStream(file).use {
                thumbnail.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } catch (e: Exception) {
            logError(e, "Couldn't save thumbnail", context)
        }
    }

    fun setStatus(status: AudioBookStatus) {
        this.status = status.value
        when (status) {
            AudioBookStatus.IN_PROGRESS -> {}
            AudioBookStatus.NOT_BEGUN -> {
                lastSavedTimestamp = 0L
                positionInTrackList = 0
                positionInTrack = 0
            }
            AudioBookStatus.FINISHED -> {
                positionInTrackList = 0
                positionInTrack = 0
            }
        }
    }

    val durationOfMostRecentTrack: Long
        get() = files?.get(positionInTrackList)?.duration ?: 0

    val totalDuration: Long
        get() {
            return files?.sumOf { it.duration } ?: 0
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
}
