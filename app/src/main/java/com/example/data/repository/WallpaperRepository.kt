package com.example.data.repository

import com.example.domain.model.Wallpaper
import com.example.domain.model.WallpaperCategory
import com.example.domain.model.WallpaperSorting
import kotlinx.coroutines.flow.Flow

sealed interface WallpaperResult<out T> {
    data class Success<T>(
        val data: T,
        val currentPage: Int,
        val lastPage: Int,
        val hasMore: Boolean
    ) : WallpaperResult<T>

    data class Error(
        val message: String,
        val isRateLimited: Boolean = false
    ) : WallpaperResult<Nothing>
}

interface WallpaperRepository {
    suspend fun getWallpapers(
        category: WallpaperCategory,
        customQuery: String? = null,
        sorting: WallpaperSorting = WallpaperSorting.TOPLIST,
        page: Int = 1,
        seed: String? = null
    ): WallpaperResult<List<Wallpaper>>

    fun getFavorites(): Flow<List<Wallpaper>>
    fun isFavorite(id: String): Flow<Boolean>
    suspend fun toggleFavorite(wallpaper: Wallpaper): Boolean
    fun clearMemoryCache()
}
