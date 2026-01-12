package com.hcmus.forumus_client.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostIdRequest(
    @Json(name = "postId") val postId: String
)

@JsonClass(generateAdapter = true)
data class PostValidationResponse(
    @Json(name = "valid") val isValid: Boolean,
    @Json(name = "message") val message: String?
)
