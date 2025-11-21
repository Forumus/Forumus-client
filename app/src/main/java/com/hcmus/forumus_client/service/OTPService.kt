package com.hcmus.forumus_client.service

import com.hcmus.forumus_client.data.model.OTP
import com.hcmus.forumus_client.utils.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class OTPService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    
    companion object {
        private const val COLLECTION_NAME = "otps"
        private const val MAX_DAILY_REQUESTS = 10 // Rate limiting
    }
    
    /**
     * Generate a 6-digit OTP code
     */
    private fun generateOTPCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }
    
    /**
     * Generate and store OTP in Firestore
     */
    suspend fun generateOTP(email: String): Resource<String> {
        return try {
            // Check if user has exceeded daily limit
            if (!canRequestOTP(email)) {
                return Resource.Error("Daily OTP limit reached. Please try again tomorrow.")
            }
            
            // Invalidate any existing OTPs for this email
            invalidateExistingOTPs(email)
            
            // Generate new OTP
            val otpCode = generateOTPCode()
            val otp = OTP.create(email, otpCode)
            
            // Store in Firestore
            firestore.collection(COLLECTION_NAME)
                .document(email)
                .set(otp)
                .await()
            
            Resource.Success(otpCode)
        } catch (e: Exception) {
            Resource.Error("Failed to generate OTP: ${e.message}")
        }
    }
    
    /**
     * Verify OTP against stored value
     */
    suspend fun verifyOTP(email: String, inputOtp: String): Resource<Boolean> {
        return try {
            val document = firestore.collection(COLLECTION_NAME)
                .document(email)
                .get()
                .await()
            
            if (!document.exists()) {
                return Resource.Error("No OTP found for this email. Please request a new one.")
            }
            
            val otp = document.toObject(OTP::class.java)
                ?: return Resource.Error("Invalid OTP data.")
            
            // Check if OTP can be verified
            if (!otp.canAttemptVerification()) {
                if (otp.isExpired()) {
                    deleteOTP(email) // Clean up expired OTP
                    return Resource.Error("OTP has expired. Please request a new one.")
                }
                if (otp.isUsed) {
                    return Resource.Error("OTP has already been used.")
                }
                if (otp.attempts >= OTP.MAX_ATTEMPTS) {
                    deleteOTP(email) // Clean up after max attempts
                    return Resource.Error("Maximum verification attempts exceeded. Please request a new OTP.")
                }
            }
            
            // Increment attempt counter
            val updatedOtp = otp.copy(attempts = otp.attempts + 1)
            
            if (otp.otpCode == inputOtp) {
                // Mark as used and update
                val successOtp = updatedOtp.copy(isUsed = true)
                firestore.collection(COLLECTION_NAME)
                    .document(email)
                    .set(successOtp)
                    .await()
                
                Resource.Success(true)
            } else {
                // Update with incremented attempts
                firestore.collection(COLLECTION_NAME)
                    .document(email)
                    .set(updatedOtp)
                    .await()
                
                val remainingAttempts = OTP.MAX_ATTEMPTS - updatedOtp.attempts
                if (remainingAttempts > 0) {
                    Resource.Error("Invalid OTP. $remainingAttempts attempts remaining.")
                } else {
                    deleteOTP(email)
                    Resource.Error("Invalid OTP. Maximum attempts exceeded. Please request a new OTP.")
                }
            }
        } catch (e: Exception) {
            Resource.Error("Verification failed: ${e.message}")
        }
    }
    
    /**
     * Check if user can request OTP (rate limiting)
     */
    private suspend fun canRequestOTP(email: String): Boolean {
        return try {
            // Get OTP requests from the last 24 hours
            val yesterday = Timestamp(Timestamp.now().seconds - 86400, 0) // 24 hours ago
            
            val query = firestore.collection("otp_requests")
                .whereEqualTo("email", email)
                .whereGreaterThan("requestedAt", yesterday)
                .get()
                .await()
            
            query.documents.size < MAX_DAILY_REQUESTS
        } catch (e: Exception) {
            true // Allow request if we can't check (fail open)
        }
    }
    
    /**
     * Log OTP request for rate limiting
     */
    suspend fun logOTPRequest(email: String) {
        try {
            val requestLog = hashMapOf(
                "email" to email,
                "requestedAt" to Timestamp.now()
            )
            
            firestore.collection("otp_requests")
                .add(requestLog)
                .await()
        } catch (e: Exception) {
            // Non-critical, continue
        }
    }
    
    /**
     * Invalidate existing OTPs for an email
     */
    private suspend fun invalidateExistingOTPs(email: String) {
        try {
            val document = firestore.collection(COLLECTION_NAME)
                .document(email)
                .get()
                .await()
            
            if (document.exists()) {
                val existingOtp = document.toObject(OTP::class.java)
                if (existingOtp != null && !existingOtp.isUsed) {
                    // Mark as used to invalidate
                    val invalidatedOtp = existingOtp.copy(isUsed = true)
                    firestore.collection(COLLECTION_NAME)
                        .document(email)
                        .set(invalidatedOtp)
                        .await()
                }
            }
        } catch (e: Exception) {
            // Non-critical, continue with new OTP generation
        }
    }
    
    /**
     * Delete OTP from Firestore
     */
    private suspend fun deleteOTP(email: String) {
        try {
            firestore.collection(COLLECTION_NAME)
                .document(email)
                .delete()
                .await()
        } catch (e: Exception) {
            // Non-critical
        }
    }
    
    /**
     * Clean up expired OTPs (call periodically)
     */
    suspend fun cleanupExpiredOTPs(): Resource<Int> {
        return try {
            val now = Timestamp.now()
            val expiredQuery = firestore.collection(COLLECTION_NAME)
                .whereLessThan("expiresAt", now)
                .get()
                .await()
            
            var deletedCount = 0
            for (document in expiredQuery.documents) {
                document.reference.delete().await()
                deletedCount++
            }
            
            Resource.Success(deletedCount)
        } catch (e: Exception) {
            Resource.Error("Cleanup failed: ${e.message}")
        }
    }
    
    /**
     * Get OTP status for debugging
     */
    suspend fun getOTPStatus(email: String): Resource<OTP> {
        return try {
            val document = firestore.collection(COLLECTION_NAME)
                .document(email)
                .get()
                .await()
            
            if (document.exists()) {
                val otp = document.toObject(OTP::class.java)
                if (otp != null) {
                    Resource.Success(otp)
                } else {
                    Resource.Error("Invalid OTP data")
                }
            } else {
                Resource.Error("No OTP found")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to get OTP status: ${e.message}")
        }
    }
}