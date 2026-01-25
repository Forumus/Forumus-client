package com.hcmus.forumus_client.data.remote

import com.hcmus.forumus_client.data.dto.EmailResponse
import com.hcmus.forumus_client.data.dto.GetSuggestedTopicsRequest
import com.hcmus.forumus_client.data.dto.GetSuggestedTopicsResponse
import com.hcmus.forumus_client.data.dto.PostSummaryRequest
import com.hcmus.forumus_client.data.dto.PostSummaryResponse
import com.hcmus.forumus_client.data.dto.ResetPasswordRequest
import com.hcmus.forumus_client.data.dto.ResetPasswordResponse
import com.hcmus.forumus_client.data.dto.SendOTPRequest
import com.hcmus.forumus_client.data.dto.SendWelcomeEmailRequest
import com.hcmus.forumus_client.utils.ApiConstants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST(ApiConstants.RESET_PASSWORD)
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>

    @POST(ApiConstants.GET_SUGGESTED_TOPICS)
    suspend fun getSuggestedTopics(
        @Body request: GetSuggestedTopicsRequest
    ): Response<GetSuggestedTopicsResponse>

    @POST(ApiConstants.SEND_OTP_EMAIL)
    suspend fun sendOTPEmail(
        @Body request: SendOTPRequest
    ): Response<EmailResponse>

    @POST(ApiConstants.SEND_WELCOME_EMAIL)
    suspend fun sendWelcomeEmail(
        @Body request: SendWelcomeEmailRequest
    ): Response<EmailResponse>

    @POST(ApiConstants.NOTIFICATIONS_TRIGGER)
    suspend fun triggerNotification(
        @Body request: com.hcmus.forumus_client.data.remote.dto.NotificationTriggerRequest
    ): Response<Unit>

    @POST(ApiConstants.VALIDATE_POST)
    suspend fun validatePost(
        @Body request: com.hcmus.forumus_client.data.dto.PostIdRequest
    ): Response<com.hcmus.forumus_client.data.dto.PostValidationResponse>

    @POST(ApiConstants.SUMMARIZE_POST)
    suspend fun summarizePost(
        @Body request: PostSummaryRequest
    ): Response<PostSummaryResponse>
}