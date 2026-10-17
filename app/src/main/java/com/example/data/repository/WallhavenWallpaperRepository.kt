package com.example.data.repository

import com.example.data.api.WallhavenApiService
import com.example.data.api.dto.WallhavenItemDto
import com.example.data.api.dto.WallhavenResponse
import com.example.data.db.WallpaperDao
import com.example.data.db.WallpaperEntity
import com.example.domain.model.Wallpaper
import com.example.domain.model.WallpaperCategory
import com.example.domain.model.WallpaperSorting
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WallhavenWallpaperRepository(
    private val wallpaperDao: WallpaperDao
) : WallpaperRepository {

    // In-memory cache for API query responses to respect 45 requests/minute rate limit
    private val memoryCache = ConcurrentHashMap<String, WallhavenResponse>()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val apiService: WallhavenApiService = Retrofit.Builder()
        .baseUrl("https://wallhaven.cc/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(WallhavenApiService::class.java)

    override fun clearMemoryCache() {
        memoryCache.clear()
    }

    override suspend fun getWallpapers(
        category: WallpaperCategory,
        customQuery: String?,
        sorting: WallpaperSorting,
        page: Int,
        seed: String?
    ): WallpaperResult<List<Wallpaper>> {
        // Resolve actual query string
        val effectiveQuery = when {
            !customQuery.isNullOrBlank() -> customQuery.trim()
            category == WallpaperCategory.DARK -> "amoled"
            category.queryKeyword != null -> category.queryKeyword
            else -> null
        }

        val topRange = if (sorting == WallpaperSorting.TOPLIST) "1M" else null
        val cacheKey = "q=${effectiveQuery ?: ""}&sort=${sorting.apiValue}&range=${topRange ?: ""}&p=$page&seed=${seed ?: ""}"

        // Return from memory cache if already fetched
        val cached = memoryCache[cacheKey]
        if (cached != null) {
            val wallpapers = cached.data.map { dto ->
                val isFav = wallpaperDao.isFavoriteDirect(dto.id)
                dto.toWallpaper(isFav)
            }
            return WallpaperResult.Success(
                data = wallpapers,
                currentPage = cached.meta.currentPage,
                lastPage = cached.meta.lastPage,
                hasMore = cached.meta.currentPage < cached.meta.lastPage
            )
        }

        // Fetch with exponential backoff on transient errors
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < 3) {
            attempt++
            try {
                val response = apiService.searchWallpapers(
                    query = effectiveQuery,
                    categories = "100", // General only
                    purity = "100",     // SFW only
                    sorting = sorting.apiValue,
                    topRange = topRange,
                    atleast = "1080x1920",
                    ratios = "9x16,9x19.5,9x20",
                    page = page,
                    seed = seed
                )

                // Cache successful response in memory
                memoryCache[cacheKey] = response

                val wallpapers = response.data.map { dto ->
                    val isFav = wallpaperDao.isFavoriteDirect(dto.id)
                    dto.toWallpaper(isFav)
                }

                return WallpaperResult.Success(
                    data = wallpapers,
                    currentPage = response.meta.currentPage,
                    lastPage = response.meta.lastPage,
                    hasMore = response.meta.currentPage < response.meta.lastPage
                )
            } catch (e: HttpException) {
                lastException = e
                if (e.code() == 429) {
                    // Rate limit hit (45 req/min) - perform backoff or return error
                    if (attempt < 3) {
                        delay(1200L * attempt) // Exponential backoff
                        continue
                    }
                    return WallpaperResult.Error(
                        message = "Slow down! Wallhaven rate limit reached (45 requests/min). Please wait a few seconds.",
                        isRateLimited = true
                    )
                }
                return WallpaperResult.Error(message = "Wallhaven server error (${e.code()}).")
            } catch (e: IOException) {
                lastException = e
                if (attempt < 2) {
                    delay(800L * attempt)
                    continue
                }
                return WallpaperResult.Error(message = "Network error: ${e.localizedMessage ?: "Check your connection"}")
            } catch (e: Exception) {
                return WallpaperResult.Error(message = "Unexpected error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }

        return WallpaperResult.Error(message = lastException?.localizedMessage ?: "Unable to load wallpapers.")
    }

    override fun getFavorites(): Flow<List<Wallpaper>> {
        return wallpaperDao.getAllFavorites().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun isFavorite(id: String): Flow<Boolean> {
        return wallpaperDao.isFavorite(id)
    }

    override suspend fun toggleFavorite(wallpaper: Wallpaper): Boolean {
        val exists = wallpaperDao.isFavoriteDirect(wallpaper.id)
        if (exists) {
            wallpaperDao.deleteFavorite(wallpaper.id)
            return false
        } else {
            wallpaperDao.insertFavorite(WallpaperEntity.fromDomainModel(wallpaper))
            return true
        }
    }

    private fun WallhavenItemDto.toWallpaper(isFav: Boolean): Wallpaper {
        val dominantColor = colors.firstOrNull()?.let {
            if (it.startsWith("#")) it else "#$it"
        }

        return Wallpaper(
            id = id,
            width = dimensionX,
            height = dimensionY,
            sourceUrl = source ?: url,
            pageUrl = url,
            avgColor = dominantColor,
            thumbnailUrl = thumbs.large,
            fullUrl = path,
            resolution = resolution,
            category = category,
            views = views,
            favoritesCount = favorites,
            isFavorite = isFav
        )
    }
}
