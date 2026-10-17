package com.example.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WallhavenResponse(
    @param:Json(name = "data") val data: List<WallhavenItemDto>,
    @param:Json(name = "meta") val meta: WallhavenMetaDto
)

@JsonClass(generateAdapter = true)
data class WallhavenItemDto(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "url") val url: String,
    @param:Json(name = "short_url") val shortUrl: String? = null,
    @param:Json(name = "views") val views: Int = 0,
    @param:Json(name = "favorites") val favorites: Int = 0,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "purity") val purity: String = "sfw",
    @param:Json(name = "category") val category: String = "general",
    @param:Json(name = "dimension_x") val dimensionX: Int = 1080,
    @param:Json(name = "dimension_y") val dimensionY: Int = 1920,
    @param:Json(name = "resolution") val resolution: String = "1080x1920",
    @param:Json(name = "ratio") val ratio: String = "0.56",
    @param:Json(name = "file_size") val fileSize: Long = 0L,
    @param:Json(name = "file_type") val fileType: String = "image/jpeg",
    @param:Json(name = "created_at") val createdAt: String = "",
    @param:Json(name = "colors") val colors: List<String> = emptyList(),
    @param:Json(name = "path") val path: String,
    @param:Json(name = "thumbs") val thumbs: WallhavenThumbsDto
)

@JsonClass(generateAdapter = true)
data class WallhavenThumbsDto(
    @param:Json(name = "large") val large: String,
    @param:Json(name = "original") val original: String? = null,
    @param:Json(name = "small") val small: String? = null
)

@JsonClass(generateAdapter = true)
data class WallhavenMetaDto(
    @param:Json(name = "current_page") val currentPage: Int = 1,
    @param:Json(name = "last_page") val lastPage: Int = 1,
    @param:Json(name = "per_page") val perPage: Int = 24,
    @param:Json(name = "total") val total: Int = 0,
    @param:Json(name = "seed") val seed: String? = null
)
