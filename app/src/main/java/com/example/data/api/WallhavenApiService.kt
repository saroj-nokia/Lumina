package com.example.data.api

import com.example.data.api.dto.WallhavenResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WallhavenApiService {

    @GET("search")
    suspend fun searchWallpapers(
        @Query("q") query: String? = null,
        @Query("categories") categories: String = "111",
        @Query("purity") purity: String = "100",
        @Query("sorting") sorting: String = "toplist",
        @Query("topRange") topRange: String? = "1y",
        @Query("atleast") atleast: String = "1080x1920",
        @Query("page") page: Int = 1,
        @Query("seed") seed: String? = null
    ): WallhavenResponse
}
