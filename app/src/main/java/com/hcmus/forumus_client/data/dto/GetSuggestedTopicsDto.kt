package com.hcmus.forumus_client.data.dto

import com.hcmus.forumus_client.data.model.Topic
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetSuggestedTopicsRequest(
    @Json(name = "title")
    val title: String,
    @Json(name = "content")
    val content: String
)

@JsonClass(generateAdapter = true)
data class GetSuggestedTopicsResponse(
    @Json(name = "success")
    val success: Boolean,
    @Json(name = "topics")
    val topics: List<Topic>
)