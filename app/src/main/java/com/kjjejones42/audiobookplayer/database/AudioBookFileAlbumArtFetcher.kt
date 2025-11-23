package com.kjjejones42.audiobookplayer.database

import android.media.MediaMetadataRetriever
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.kjjejones42.audiobookplayer.database.models.AudioBookFile
import okio.Buffer
import okio.FileSystem

class AudioBookFileAlbumArtFetcher(
    private val data: AudioBookFile,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val retriever = MediaMetadataRetriever()

        try {
            options.context.contentResolver.openAssetFileDescriptor(data.uri, "r")?.use { afd ->
                retriever.setDataSource(afd.fileDescriptor)
                val embeddedPicture = retriever.embeddedPicture
                if (embeddedPicture != null) {
                    val buffer = Buffer().write(embeddedPicture)
                    return SourceFetchResult(
                        source = ImageSource(
                            source = buffer,
                            fileSystem = FileSystem.SYSTEM,
                            metadata = null
                        ),
                        mimeType = "image/jpeg",
                        dataSource = DataSource.DISK
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        return null
    }

    class Factory : Fetcher.Factory<AudioBookFile> {
        override fun create(
            data: AudioBookFile,
            options: Options,
            imageLoader: coil3.ImageLoader
        ): Fetcher = AudioBookFileAlbumArtFetcher(data, options)
    }
}