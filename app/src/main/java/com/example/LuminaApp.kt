package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.db.WallpaperDatabase
import com.example.data.repository.WallpaperRepository
import com.example.data.repository.WallhavenWallpaperRepository
import com.example.util.WallpaperManagerHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class LuminaApp : Application(), ImageLoaderFactory {

    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var database: WallpaperDatabase
        private set

    lateinit var repository: WallpaperRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val okHttpBuilder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            okHttpBuilder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
        }

        okHttpClient = okHttpBuilder.build()
        database = WallpaperDatabase.getDatabase(this)
        repository = WallhavenWallpaperRepository(
            wallpaperDao = database.wallpaperDao(),
            okHttpClient = okHttpClient
        )
        WallpaperManagerHelper.clearOldCache(this)
    }

    // One shared image loader that reuses the app's OkHttp client (same connection pool and timeouts).
    // Wallpapers never change, so always keep them in the disk cache even if the server sends no-cache headers.
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .callFactory(okHttpClient)
        .respectCacheHeaders(false)
        .build()
}
