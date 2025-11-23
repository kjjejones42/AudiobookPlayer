package com.kjjejones42.audiobookplayer.display

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.ArraySet
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kjjejones42.audiobookplayer.database.AudiobookDatabase.Companion.getInstance
import com.kjjejones42.audiobookplayer.database.models.AudioBook
import com.kjjejones42.audiobookplayer.database.models.AudioBookFile
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

    private fun parseBook(rel: String, mediaFiles: List<AudioBookFile>): AudioBook {
        val directory = File(rel).name
        val imagePath = findImage(rel)
        val author = mediaFiles.firstNotNullOfOrNull { getFileAuthor(it.uri) } ?: ""
        return AudioBook(directory, rel, imagePath, mediaFiles, author)
    }

    @SuppressLint("Range")
    private fun queryBooks(): List<AudioBook?> {
        val result = ArrayList<AudioBook>()
        val selection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DURATION
        )

        val where = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("audio/%", "Audiobooks/%")

        for (uri in listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Audio.Media.INTERNAL_CONTENT_URI)) {
            val cursor = applicationContext.contentResolver.query(uri, selection, where, selectionArgs, null)
            cursor?.use {
                it.apply {
                    val idColumn = getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val dirColumn = getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                    val titleColumn = getColumnIndex(MediaStore.Files.FileColumns.TITLE)
                    val durationColumn = getColumnIndex(MediaStore.Files.FileColumns.DURATION)
                    val fileNameColumn = getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)

                    val dirs = HashMap<String, MutableList<AudioBookFile>>()
                    while (moveToNext()) {
                        val id = getLong(idColumn)
                        val dir = getString(dirColumn) ?: continue
                        val title = getString(titleColumn)
                        val duration = getInt(durationColumn)
                        val fileName = getString(fileNameColumn)
                        val mediaUri = ContentUris.withAppendedId(uri, id)
                        val media = AudioBookFile(mediaUri, title, fileName, duration.toLong())
                        dirs.computeIfAbsent(dir) { ArrayList() }.add(media)
                    }
                    val uriResult = dirs.map { (key, value) -> parseBook(key, value) }
                    result.addAll(uriResult)
                }
            }
        }
        return result
    }

    override fun doWork(): Result {
        return try {
            val books = queryBooks()
            val ids = books.filterNotNull().mapNotNull { it.baseDir }.toSet()
            val dao = getInstance(applicationContext).audiobookDao()
            val dbIds: MutableSet<String?> = ArraySet(dao.allBaseDirs)
            dbIds.removeAll(ids)
            for (dbId in dbIds) {
                dao.delete(dbId)
            }
            dao.insertAll(books.filterNotNull())
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

