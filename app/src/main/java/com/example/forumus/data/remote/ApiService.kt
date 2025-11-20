package com.example.forumus.data.remote

import com.example.forumus.data.model.ResetPasswordRequest
import com.example.forumus.data.model.ResetPasswordResponse
import com.example.forumus.utils.ApiConstants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST(ApiConstants.RESET_PASSWORD)
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ResetPasswordResponse>
}