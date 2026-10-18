package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.view.WindowManager
import androidx.core.content.FileProvider
import com.example.LuminaApp
import com.example.domain.model.WallpaperDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

object WallpaperManagerHelper {

    private const val CACHE_DIR = "wallpapers"
    private const val MAX_CACHE_AGE_MS = 24L * 60 * 60 * 1000

    /** Portrait (width, height) in px of the whole display, whatever the rotation. */
    fun targetSize(context: Context): Pair<Int, Int> {
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        return minOf(bounds.width(), bounds.height()) to maxOf(bounds.width(), bounds.height())
    }

    suspend fun applyWallpaper(
        context: Context,
        imageUrl: String,
        destination: WallpaperDestination,
        crop: CropState
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            val app = context.applicationContext
            val file = fetchOriginal(app, imageUrl)
            val (targetW, targetH) = targetSize(app)
            bitmap = decodeCrop(file, crop, targetW, targetH)

            val flags = when (destination) {
                WallpaperDestination.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperDestination.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperDestination.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            WallpaperManager.getInstance(app).setBitmap(bitmap, null, true, flags)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Result.failure(IllegalStateException("This image is too large for your device"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            bitmap?.recycle() // we created it, Coil is not involved any more
        }
    }

    /** Downloads the original (uncropped) file and returns a content:// uri the system picker can read. */
    suspend fun prepareForSystemPicker(context: Context, imageUrl: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            try {
                val app = context.applicationContext
                val file = fetchOriginal(app, imageUrl)
                Result.success(FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** Blocking. Call from Dispatchers.IO. Reuses the file if it is already cached. */
    private fun fetchOriginal(context: Context, url: String): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val name = url.substringBefore('?').substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifEmpty { "image" }
        val file = File(dir, name)
        if (file.length() > 0) return file

        val part = File(dir, "$name.part")
        try {
            val client = (context as? LuminaApp)?.okHttpClient ?: OkHttpClient()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to download image: HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty response body")
                body.byteStream().use { input -> part.outputStream().use { output -> input.copyTo(output) } }
            }
            if (!part.renameTo(file)) throw IOException("Could not save image")
        } finally {
            part.delete() // only does something when the download or rename failed
        }
        return file
    }

    /** Decodes only the chosen part of the file, sized to exactly targetW x targetH. */
    private fun decodeCrop(file: File, crop: CropState, targetW: Int, targetH: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Could not read the image")

        val r = computeCropRect(bounds.outWidth, bounds.outHeight, targetW.toFloat() / targetH, crop)
        var sample = 1
        while (r.width / (sample * 2) >= targetW && r.height / (sample * 2) >= targetH) sample *= 2

        val decoder = BitmapRegionDecoder.newInstance(file.path)
        try {
            val region = decoder.decodeRegion(
                Rect(r.left, r.top, r.left + r.width, r.top + r.height),
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: throw IOException("Could not decode the image")
            if (region.width == targetW && region.height == targetH) return region
            return Bitmap.createScaledBitmap(region, targetW, targetH, true).also {
                if (it !== region) region.recycle()
            }
        } finally {
            decoder.recycle()
        }
    }

    /** Drops downloaded originals older than 24 hours. Safe to call on any thread. */
    fun clearOldCache(context: Context) {
        thread {
            val cutoff = System.currentTimeMillis() - MAX_CACHE_AGE_MS
            File(context.cacheDir, CACHE_DIR).listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
        }
    }
}
