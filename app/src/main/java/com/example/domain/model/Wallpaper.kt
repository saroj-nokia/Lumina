package com.example.domain.model

data class Wallpaper(
    val id: String,
    val width: Int,
    val height: Int,
    val sourceUrl: String,
    val pageUrl: String,
    val avgColor: String?,
    val thumbnailUrl: String,
    val fullUrl: String,
    val resolution: String = "",
    val category: String = "General",
    val views: Int = 0,
    val favoritesCount: Int = 0,
    val isFavorite: Boolean = false
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 0.5625f
}

enum class WallpaperCategory(val title: String, val queryKeyword: String?) {
    FEATURED("Featured", null),
    NATURE("Nature", "nature"),
    ABSTRACT("Abstract", "abstract"),
    DARK("Dark/AMOLED", "amoled"),
    MINIMAL("Minimal", "minimal"),
    SPACE("Space", "space"),
    CITIES("Cities", "city"),
    ANIMALS("Animals", "animals"),
    CARS("Cars", "cars"),
    ARCHITECTURE("Architecture", "architecture")
}

enum class WallpaperSorting(val label: String, val apiValue: String) {
    TOPLIST("Top Rated", "toplist"),
    DATE_ADDED("Latest", "date_added"),
    VIEWS("Most Viewed", "views"),
    RANDOM("Random", "random")
}

enum class WallpaperDestination {
    HOME,
    LOCK,
    BOTH
}
