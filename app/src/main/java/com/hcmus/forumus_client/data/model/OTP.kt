package com.hcmus.forumus_client.data.model

import com.google.firebase.Timestamp

data class OTP(
    val email: String = "",
    val otpCode: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val isUsed: Boolean = false,
    val attempts: Int = 0 // Track verification attempts for security
) {
    companion object {
        const val EXPIRY_MINUTES = 5L
        const val MAX_ATTEMPTS = 3
        
        fun create(email: String, otpCode: String): OTP {
            val now = Timestamp.now()
            val expiryTime = Timestamp(now.seconds + (EXPIRY_MINUTES * 60), now.nanoseconds)
            
            return OTP(
                email = email,
                otpCode = otpCode,
                createdAt = now,
                expiresAt = expiryTime,
                isUsed = false,
                attempts = 0
            )
        }
    }
    
    fun isExpired(): Boolean {
        return Timestamp.now().seconds > expiresAt.seconds
    }
    
    fun canAttemptVerification(): Boolean {
        return !isUsed && !isExpired() && attempts < MAX_ATTEMPTS
    }
}