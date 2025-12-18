package com.hcmus.forumus_client.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendOTPRequest(
    @Json(name = "recipientEmail")
    val recipientEmail: String,
    @Json(name = "otpCode")
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class SendWelcomeEmailRequest(
    @Json(name = "recipientEmail")
    val recipientEmail: String,
    @Json(name = "username")
    val username: String
)

@JsonClass(generateAdapter = true)
data class EmailResponse(
    @Json(name = "success")
    val success: Boolean,
    @Json(name = "message")
    val message: String
)
