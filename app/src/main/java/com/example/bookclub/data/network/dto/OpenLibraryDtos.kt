package com.example.bookclub.data.network

import com.squareup.moshi.Json

data class SearchResponseDto(
    val docs: List<DocDto> = emptyList()
)

// DTO pentru lucrari
data class DocDto(
    val key: String?,
    val title: String?,
    @Json(name = "author_name") val authorName: List<String>?,
    @Json(name = "cover_i") val coverId: Int?
)
// DTO pentru detalii lucrare
data class WorkDetailsDto(
    val title: String?,
    val description: Any?,                 // poate fi String sau { value: String }
    val subjects: List<String>?,
    val covers: List<Int>?
)
