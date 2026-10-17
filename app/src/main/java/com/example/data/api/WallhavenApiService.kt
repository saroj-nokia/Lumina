package com.example.data.api

import com.example.data.api.dto.WallhavenResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WallhavenApiService {

    @GET("search")
    suspend fun searchWallpapers(
        @Query("q") query: String? = null,
        @Query("categories") categories: String = "100",
        @Query("purity") purity: String = "100",
        @Query("sorting") sorting: String = "toplist",
        @Query("topRange") topRange: String? = "1M",
        @Query("atleast") atleast: String = "1080x1920",
        @Query("ratios") ratios: String = "9x16,9x19.5,9x20",
        @Query("page") page: Int = 1,
        @Query("seed") seed: String? = null
    ): WallhavenResponse
}
