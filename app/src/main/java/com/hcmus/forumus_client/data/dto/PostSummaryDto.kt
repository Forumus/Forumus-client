package com.hcmus.forumus_client.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request DTO for generating an AI summary of a post.
 */
@JsonClass(generateAdapter = true)
data class PostSummaryRequest(
    @Json(name = "postId") val postId: String
)

/**
 * Response DTO for AI-generated post summaries.
 */
@JsonClass(generateAdapter = true)
data class PostSummaryResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "summary") val summary: String?,
    @Json(name = "errorMessage") val errorMessage: String?,
    @Json(name = "cached") val cached: Boolean = false
)
