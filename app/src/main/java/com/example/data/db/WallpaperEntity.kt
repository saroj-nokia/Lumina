package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Wallpaper

@Entity(tableName = "favorite_wallpapers")
data class WallpaperEntity(
    @PrimaryKey
    val id: String,
    val width: Int,
    val height: Int,
    val sourceUrl: String,
    val pageUrl: String,
    val avgColor: String?,
    val thumbnailUrl: String,
    val fullUrl: String,
    val resolution: String,
    val category: String,
    val savedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Wallpaper {
        return Wallpaper(
            id = id,
            width = width,
            height = height,
            sourceUrl = sourceUrl,
            pageUrl = pageUrl,
            avgColor = avgColor,
            thumbnailUrl = thumbnailUrl,
            fullUrl = fullUrl,
            resolution = resolution,
            category = category,
            isFavorite = true
        )
    }

    companion object {
        fun fromDomainModel(wallpaper: Wallpaper): WallpaperEntity {
            return WallpaperEntity(
                id = wallpaper.id,
                width = wallpaper.width,
                height = wallpaper.height,
                sourceUrl = wallpaper.sourceUrl,
                pageUrl = wallpaper.pageUrl,
                avgColor = wallpaper.avgColor,
                thumbnailUrl = wallpaper.thumbnailUrl,
                fullUrl = wallpaper.fullUrl,
                resolution = wallpaper.resolution,
                category = wallpaper.category
            )
        }
    }
}
