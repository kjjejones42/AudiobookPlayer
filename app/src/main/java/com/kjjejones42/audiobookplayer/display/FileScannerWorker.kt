package com.kjjejones42.audiobookplayer.display

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.ArraySet
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kjjejones42.audiobookplayer.AudioBook
import com.kjjejones42.audiobookplayer.MediaItem
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance
import com.kjjejones42.audiobookplayer.logError
import java.io.File

class FileScannerWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private fun getFileAuthor(filename: Uri): String? {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = authorFields
        val where = "${MediaStore.Audio.Media._ID} = ?"
        val args = arrayOf(filename.lastPathSegment)
        applicationContext.contentResolver.query(uri, selection, where, args, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                for (i in 0..<cursor.columnCount) {
                    cursor.getString(i)
                        ?.takeIf { it.isNotEmpty() && (it != "<unknown>")  }
                        ?.let { return it }
                }
            }
        }
        return null
    }

    @SuppressLint("Range")
    private fun findImage(directory: String): String? {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = arrayOf(MediaStore.Images.Media.DATA)
        val where = "${MediaStore.Images.Media.RELATIVE_PATH} = ?"
        val args = arrayOf(directory)
        applicationContext.contentResolver.query(uri, selection, where, args, null)?.use {
            while (it.moveToNext()) {
                val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                it.getString(columnIndex)?.let {
                    return it
                }
            }
        }
        return null
    }

    private fun parseBook(rel: String, mediaFiles: List<MediaItem>): AudioBook {
        val directory = File(rel).name
        val imagePath = findImage(rel)
        val author = mediaFiles.firstNotNullOfOrNull { getFileAuthor(it.uri) } ?: ""
        return AudioBook(directory, rel, imagePath, mediaFiles, author)
    }

    @get:SuppressLint("Range")
    private val list: List<AudioBook?>
        get() {
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val selection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.MediaColumns.DISPLAY_NAME,
            )
            val where = "${MediaStore.Audio.Media.IS_AUDIOBOOK} != 0"
            applicationContext.contentResolver.query(uri, selection, where, null, null)?.use {
                it.apply {
                    val dirs = HashMap<String, MutableList<MediaItem>>()
                    while (moveToNext()) {
                        val id = getLong(getColumnIndex(MediaStore.Audio.Media._ID))
                        val dir = getString(1)
                        val title = getString(2)
                        val duration = getInt(3)
                        val fileName = getString(4)
                        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        val media = MediaItem(uri, title, fileName, duration.toLong())
                        dirs.computeIfAbsent(dir) { ArrayList() }.add(media)
                    }
                    return dirs.map { (key, value) -> parseBook(key, value) }
                }
            }
            return ArrayList()
        }

    override fun doWork(): Result {
        return try {
            val ids = list.filterNotNull().mapNotNull { it.baseDir }.toSet()
            val dao = getInstance(applicationContext).audiobookDao()
            val dbIds: MutableSet<String?> = ArraySet(dao.allBaseDirs)
            dbIds.removeAll(ids)
            for (dbId in dbIds) {
                dao.delete(dbId)
            }
            dao.insertAll(list.filterNotNull())
            Result.success()
        } catch (e: Exception) {
            logError(e, "Error scanning files", applicationContext)
            Result.failure()
        }
    }

    companion object {
        private val authorFields by lazy {
            arrayOf(
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.AUTHOR,
                MediaStore.Audio.Media.COMPOSER,
                MediaStore.Audio.Media.WRITER
            )
        }
    }
}

