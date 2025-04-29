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
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Collections
import kotlin.math.ceil
import kotlin.math.max

@Entity
class AudioBook {
    @JvmField
    @PrimaryKey
    var displayName: String

    @JvmField
    @ColumnInfo
    var baseDir: String? = null

    @JvmField
    @ColumnInfo
    var files: List<MediaItem>? = null

    @JvmField
    @ColumnInfo
    var author: String? = null

    @JvmField
    @ColumnInfo
    var imagePath: String? = null

    @JvmField
    @ColumnInfo
    var lastSavedTimestamp: Long = 0

    @JvmField
    @ColumnInfo
    var positionInTrack: Int = 0

    @JvmField
    @ColumnInfo
    var positionInTrackList: Int = 0

    @ColumnInfo
    private var status = 0

    @Transient
    var isArtGenerated: Boolean = false
        private set

    @Transient
    private var thumbnail: Bitmap? = null

    @Transient
    private var art: Bitmap? = null

    @Transient
    private var albumArtPalette: Palette? = null

    constructor() {
        displayName = ""
    }

    constructor(
        name: String,
        baseDir: String?,
        imagePath: String?,
        files: List<MediaItem>?,
        author: String?
    ) {
        if (files != null) {
            Collections.sort(files)
        }
        this.baseDir = baseDir
        this.imagePath = imagePath
        this.displayName = name
        this.files = files
        this.status = STATUS_NOT_BEGUN
        this.author = author ?: ""
    }

    fun getAlbumArtPalette(context: Context): Palette? {
        if (albumArtPalette == null) {
            generatePalette(getAlbumArt(context))
        }
        return albumArtPalette
    }

    private fun generatePalette(bitmap: Bitmap?) {
        if (bitmap != null) {
            albumArtPalette = Palette.from(bitmap).generate()
        }
    }

    fun getAlbumArt(context: Context): Bitmap {
        if (art != null) {
            return art as Bitmap
        }
        var result: Bitmap? = null
        try {
            if (imagePath != null && imagePath!!.isNotEmpty()) {
                val fis = FileInputStream(imagePath)
                result = BitmapFactory.decodeStream(fis)
                fis.close()
            }
        } catch (e: FileNotFoundException) {
            logError(e, "Couldn't get album art", context)
        } catch (ignored: Exception) {
        }
        if (result == null) {
            for (file in files!!) {
                result = file.getEmbeddedPicture(context)
                if (result != null) {
                    break
                }
            }
        }
        if (result == null) {
            result = getGeneratedAlbumArt(displayName.substring(0, 1))
        }
        art = result
        generatePalette(art)
        return art!!
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
        if (thumbnail == null) {
            loadThumbnail(activity)
            if (thumbnail == null) {
                generateThumbnailFromAlbumArt(activity, getAlbumArt(activity))
            }
        }
        return thumbnail
    }

    private fun getThumbnailFile(context: Context): File {
        return File(context.cacheDir, "$uniqueId.thumbnail")
    }

    private fun generateThumbnailFromAlbumArt(activity: Activity, bitmap: Bitmap) {
        val size = getThumbnailSize(activity)
        thumbnail = ThumbnailUtils.extractThumbnail(bitmap, size, size)
        if (thumbnail != null) {
            saveThumbnail(activity)
        }
    }

    private fun loadThumbnail(context: Context) {
        try {
            val file = getThumbnailFile(context)
            val fis = FileInputStream(file)
            thumbnail = BitmapFactory.decodeStream(fis)
            fis.close()
        } catch (ignored: FileNotFoundException) {
        } catch (e: Exception) {
            logError(e, "Couldn't load thumbnail", context)
        }
    }

    private fun saveThumbnail(context: Context) {
        try {
            val file = getThumbnailFile(context)
            val fos = FileOutputStream(file)
            thumbnail!!.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        } catch (e: Exception) {
            logError(e, "Couldn't save thumbnail", context)
        }
    }

    fun getStatus(): Int {
        return status
    }

    fun setStatus(status: Int) {
        when (status) {
            STATUS_IN_PROGRESS -> {}
            STATUS_NOT_BEGUN -> {
                lastSavedTimestamp = 0L
                positionInTrackList = 0
                positionInTrack = 0
            }

            STATUS_FINISHED -> {
                positionInTrackList = 0
                positionInTrack = 0
            }
        }
        this.status = status
    }

    val durationOfMostRecentTrack: Long
        get() = files!![positionInTrackList].duration

    val totalDuration: Long
        get() {
            var total: Long = 0
            for (item in files!!) {
                total += item.duration
            }
            return total
        }

    override fun equals(other: Any?): Boolean {
        if (other is AudioBook) {
            return this.uniqueId == other.uniqueId
        }
        return super.equals(other)
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

    companion object {
        const val STATUS_IN_PROGRESS: Int = 0
        const val STATUS_NOT_BEGUN: Int = 1
        const val STATUS_FINISHED: Int = 2
        private var map: HashMap<Int, String>? = null
        private var thumbnailSize = 0
        @JvmStatic
        val statusMap: HashMap<Int, String>?
            get() {
                if (map == null) {
                    map = HashMap()
                    map!![STATUS_FINISHED] = "Finished"
                    map!![STATUS_IN_PROGRESS] = "In Progress"
                    map!![STATUS_NOT_BEGUN] = "Not Begun"
                }
                return map
            }

        private fun getThumbnailSize(activity: Activity): Int {
            if (thumbnailSize == 0) {
                activity.theme.obtainStyledAttributes(
                    intArrayOf(android.R.attr.listPreferredItemHeight)
                ).use { value ->
                    val height = value.getDimension(0, .0f)
                    thumbnailSize = Math.round(ceil(height.toDouble())).toInt()
                }
            }
            return thumbnailSize
        }
    }
}
