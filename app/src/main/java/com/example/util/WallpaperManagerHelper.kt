package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.example.domain.model.WallpaperDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperManagerHelper {

    suspend fun applyWallpaper(
        context: Context,
        imageUrl: String,
        destination: WallpaperDestination
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val wm = WallpaperManager.getInstance(context)
            val displayMetrics = context.resources.displayMetrics
            val desiredWidth = wm.desiredMinimumWidth
            val desiredHeight = wm.desiredMinimumHeight

            val targetWidth = if (desiredWidth > 0 && desiredHeight > 0) desiredWidth else displayMetrics.widthPixels
            val targetHeight = if (desiredWidth > 0 && desiredHeight > 0) desiredHeight else displayMetrics.heightPixels

            val imageLoader = context.applicationContext.imageLoader
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(targetWidth, targetHeight)
                .scale(Scale.FILL)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) {
                return@withContext Result.failure(IllegalStateException("Failed to download wallpaper image."))
            }

            val sourceBitmap = (result.drawable as? BitmapDrawable)?.bitmap
                ?: return@withContext Result.failure(IllegalStateException("Failed to decode bitmap."))

            val finalBitmap = cropToScreenAspectRatio(sourceBitmap, targetWidth, targetHeight)

            val flags = when (destination) {
                WallpaperDestination.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperDestination.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperDestination.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(finalBitmap, null, true, flags)
            } else {
                wm.setBitmap(finalBitmap)
            }

            // Recycle only bitmaps this function created, never the Coil-owned source bitmap
            if (finalBitmap !== sourceBitmap && !finalBitmap.isRecycled) {
                finalBitmap.recycle()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Result.failure(IllegalStateException("This image is too large for your device"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cropToScreenAspectRatio(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
        val sourceAspect = source.width.toFloat() / source.height.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val cropX: Int
        val cropY: Int

        if (sourceAspect > targetAspect) {
            cropHeight = source.height
            cropWidth = (source.height * targetAspect).toInt().coerceAtMost(source.width)
            cropX = (source.width - cropWidth) / 2
            cropY = 0
        } else {
            cropWidth = source.width
            cropHeight = (source.width / targetAspect).toInt().coerceAtMost(source.height)
            cropX = 0
            cropY = (source.height - cropHeight) / 2
        }

        return if (cropWidth == source.width && cropHeight == source.height) {
            source
        } else {
            Bitmap.createBitmap(source, cropX, cropY, cropWidth, cropHeight)
        }
    }
}
