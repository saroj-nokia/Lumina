package com.example

import android.app.Application
import com.example.data.db.WallpaperDatabase
import com.example.data.repository.WallpaperRepository
import com.example.data.repository.WallhavenWallpaperRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class LuminaApp : Application() {

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
    }
}
