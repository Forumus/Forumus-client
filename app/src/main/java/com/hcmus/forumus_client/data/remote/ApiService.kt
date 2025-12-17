package com.hcmus.forumus_client.data.remote

import com.hcmus.forumus_client.data.dto.GetSuggestedTopicsRequest
import com.hcmus.forumus_client.data.dto.GetSuggestedTopicsResponse
import com.hcmus.forumus_client.data.dto.ResetPasswordRequest
import com.hcmus.forumus_client.data.dto.ResetPasswordResponse
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
}