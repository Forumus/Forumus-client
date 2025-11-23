package com.hcmus.forumus_client.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    @Json(name = "secretKey")
    val secretKey: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "newPassword")
    val newPassword: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordResponse(
    @Json(name = "success")
    val success: Boolean,
    @Json(name = "message")
    val message: String
)