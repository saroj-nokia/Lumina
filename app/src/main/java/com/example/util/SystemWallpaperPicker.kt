package com.example.util

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Hands the uncropped image to the phone's own wallpaper screen. What happens next depends on the ROM. */
object SystemWallpaperPicker {

    /** Returns false when nothing on this device accepted the image. Never throws. */
    fun launch(context: Context, uri: Uri): Boolean {
        val crop = try {
            WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(uri)
        } catch (_: IllegalArgumentException) {
            null // the launcher has no crop-and-set screen
        }
        if (crop != null && start(context, crop.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))) return true

        val attach = Intent(Intent.ACTION_ATTACH_DATA)
            .setDataAndType(uri, "image/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        attach.clipData = ClipData.newRawUri(null, uri)
        return start(context, Intent.createChooser(attach, "Set as"))
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
