package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

object AudioFileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var fileName: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (fileName == null) {
            val path = uri.path
            val cut = path?.lastIndexOf('/') ?: -1
            if (cut != -1 && path != null) {
                fileName = path.substring(cut + 1)
            }
        }
        return fileName?.takeIf { it.isNotBlank() } ?: "本地音乐.mp3"
    }

    fun getAudioDurationMs(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull()?.coerceAtLeast(10000L) ?: 45000L
        } catch (_: Exception) {
            45000L
        }
    }

    fun getAudioArtist(context: Context, uri: Uri): String {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val artistStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            retriever.release()
            artistStr?.takeIf { it.isNotBlank() } ?: "本地MP3"
        } catch (_: Exception) {
            "本地MP3"
        }
    }
}
