package com.example.bookclub.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit API spre openlibrary.org
interface OpenLibraryApi {
    // Endpoint căutare lucrari
    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): SearchResponseDto

    // Endpoint detalii lucrare dupa id
    @GET("works/{id}.json")
    suspend fun workDetails(@Path("id") workId: String): WorkDetailsDto
}