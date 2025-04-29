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
import java.util.stream.Collectors

class FileScannerWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private fun getFileAuthor(filename: Uri): String? {
        applicationContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            authorFields,
            MediaStore.Audio.Media._ID + " = ?",
            arrayOf(filename.lastPathSegment),
            null
        ).use { cursor ->
            if (cursor == null) return null
            while (cursor.moveToNext()) {
                for (i in 0..<cursor.columnCount) {
                    val result = cursor.getString(i)
                    if (result != null && result.isNotEmpty() && (result != "<unknown>")) {
                        return result
                    }
                }
            }
        }
        return null
    }

    private fun findAuthor(mediaFiles: Collection<MediaItem>): String {
        for (item in mediaFiles) {
            val author = getFileAuthor(item.getUri())
            if (author != null) {
                return author
            }
        }
        return ""
    }

    @SuppressLint("Range")
    private fun findImage(directory: String): String? {
        val cursor = applicationContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DATA),
            MediaStore.Images.Media.RELATIVE_PATH + " = ?",
            arrayOf(directory),
            null
        )
        var result: String? = null
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return result
    }

    private fun parseBook(rel: String, mediaFiles: List<MediaItem>): AudioBook {
        val directory = File(rel).name
        val imagePath = findImage(rel)
        val author = findAuthor(mediaFiles)
        return AudioBook(directory, rel, imagePath, mediaFiles, author)
    }

    @get:SuppressLint("Range")
    private val list: List<AudioBook?>
        get() {
            val results: MutableList<AudioBook?> =
                ArrayList()

            val cursor =
                applicationContext.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.RELATIVE_PATH,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                    ),
                    MediaStore.Audio.Media.IS_AUDIOBOOK + " != 0",
                    null,
                    null
                )
            if (cursor != null) {
                val dirs: MutableMap<String, MutableList<MediaItem>> =
                    HashMap()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val dir = cursor.getString(1)
                    val title = cursor.getString(2)
                    val duration = cursor.getInt(3)
                    val uri =
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val media =
                        MediaItem(uri, title, duration.toLong())
                    if (!dirs.containsKey(dir)) {
                        dirs[dir] = ArrayList()
                    }
                    dirs[dir]?.add(media)
                }
                cursor.close()
                for ((key, value) in dirs) {
                    val result = parseBook(key, value)
                    results.add(result)
                }
            }
            return results
        }

    override fun doWork(): Result {
        try {
            val results = list
            val ids =
                results.stream().map { x: AudioBook? -> x!!.baseDir }.collect(Collectors.toSet())
            val dao = getInstance(applicationContext).audiobookDao()
            val dbIds: MutableSet<String?> = ArraySet(
                dao!!.allBaseDirs
            )
            dbIds.removeAll(ids)
            for (dbId in dbIds) {
                dao.delete(dbId)
            }
            dao.insertAll(results)
        } catch (e: Exception) {
            logError(
                e, "Error scanning files",
                applicationContext
            )
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        private val authorFields = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.AUTHOR,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.WRITER
        )
    }
}

