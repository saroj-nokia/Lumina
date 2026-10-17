package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    @Query("SELECT * FROM favorite_wallpapers ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<WallpaperEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_wallpapers WHERE id = :id)")
    fun isFavorite(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_wallpapers WHERE id = :id)")
    suspend fun isFavoriteDirect(id: String): Boolean

    @Query("SELECT id FROM favorite_wallpapers WHERE id IN (:ids)")
    suspend fun getFavoriteIds(ids: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(wallpaper: WallpaperEntity)

    @Query("DELETE FROM favorite_wallpapers WHERE id = :id")
    suspend fun deleteFavorite(id: String)
}
