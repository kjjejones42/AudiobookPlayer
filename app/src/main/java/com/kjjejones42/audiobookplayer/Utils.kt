package com.kjjejones42.audiobookplayer

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private var logFileUri: Uri? = null
private const val fileName = "AudioBookPlayerError.log"

fun logError(e: Throwable, context: Context?) {
    logError(e, "", context)
}

fun logError(e: Throwable, message: String, context: Context?) {
    writeToFile(e, message, context)
}

private fun writeToFile(e: Throwable, message: String, context: Context?) {
    val tempContext = context ?: AppApplication.context
    try {
        val uri = checkNotNull(getLogFileUri(tempContext))
        val fos = checkNotNull(
            tempContext.contentResolver.openOutputStream(uri, "wa")
        )
        val printWriter = PrintWriter(fos)
        printWriter.write(currentDateTime() + "\n")
        printWriter.write(message + "\n")
        e.printStackTrace(printWriter)
        printWriter.write("\n--------------------\n")
        printWriter.close()
        Toast.makeText(tempContext, "Uncaught exception written to log", Toast.LENGTH_SHORT).show()
    } catch (ex: FileNotFoundException) {
        throw RuntimeException(ex)
    }
}

private fun getLogFileUri(context: Context): Uri? {
    if (logFileUri == null) {
        val uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val relativePath =
            String.format("%s/%s/", Environment.DIRECTORY_DOCUMENTS, context.packageName)
        val resolver = context.contentResolver
        logFileUri = queryLogFileUri(resolver, uri, relativePath)
        if (logFileUri == null) {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            logFileUri = resolver.insert(uri, contentValues)
        }
    }
    return logFileUri
}

private fun currentDateTime(): String {
    val format = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS z", Locale.getDefault())
    return format.format(Date())
}

private fun queryLogFileUri(
    resolver: ContentResolver, uri: Uri, relativePath: String
): Uri? {
    val selection = arrayOf(MediaStore.MediaColumns._ID)
    val where = String.format(
        "%s = ? AND %s = ?",
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.RELATIVE_PATH
    )
    val args = arrayOf(fileName, relativePath)
    resolver.query(uri, selection, where, args, null).use { cursor ->
        if (cursor != null && cursor.moveToNext()) {
            val id = cursor.getLong(0)
            return ContentUris.withAppendedId(uri, id)
        }
    }
    return null
}