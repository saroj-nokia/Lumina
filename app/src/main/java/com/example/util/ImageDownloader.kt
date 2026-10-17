package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.LuminaApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ImageDownloader {

    suspend fun saveToGallery(
        context: Context,
        imageUrl: String,
        photoId: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val resolver = appContext.contentResolver
        var insertedUri: Uri? = null
        var streamCompleted = false

        try {
            val okHttpClient = (appContext as? LuminaApp)?.okHttpClient ?: OkHttpClient()
            val request = Request.Builder().url(imageUrl).build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to download image: HTTP ${response.code}"))
                }

                val body = response.body
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val contentType = response.header("Content-Type")?.lowercase()
                val (mimeType, extension) = when {
                    contentType?.contains("image/png") == true -> "image/png" to "png"
                    contentType?.contains("image/webp") == true -> "image/webp" to "webp"
                    contentType?.contains("image/jpeg") == true || contentType?.contains("image/jpg") == true -> "image/jpeg" to "jpg"
                    else -> {
                        val urlExt = imageUrl.substringAfterLast('.', "").substringBefore('?').lowercase()
                        when (urlExt) {
                            "png" -> "image/png" to "png"
                            "webp" -> "image/webp" to "webp"
                            else -> "image/jpeg" to "jpg"
                        }
                    }
                }

                val filename = "Lumina_Wallpaper_${photoId}_${System.currentTimeMillis()}.$extension"

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/Wallpapers"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext Result.failure(IllegalStateException("Could not create MediaStore entry"))
                insertedUri = uri

                resolver.openOutputStream(uri)?.use { outputStream ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IllegalStateException("Could not open output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                streamCompleted = true
                Result.success(uri)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            val uriToDelete = insertedUri
            if (!streamCompleted && uriToDelete != null) {
                try {
                    resolver.delete(uriToDelete, null, null)
                } catch (_: Exception) {
                }
            }
        }
    }
}
