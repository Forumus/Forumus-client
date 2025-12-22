package com.hcmus.forumus_client.data.remote

import com.hcmus.forumus_client.data.model.ResetPasswordRequest
import com.hcmus.forumus_client.data.model.ResetPasswordResponse
import com.hcmus.forumus_client.utils.ApiConstants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST(ApiConstants.RESET_PASSWORD)
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>

    @POST(ApiConstants.NOTIFICATIONS_TRIGGER)
    suspend fun triggerNotification(
        @Body request: com.hcmus.forumus_client.data.remote.dto.NotificationTriggerRequest
    ): Response<Unit>
}