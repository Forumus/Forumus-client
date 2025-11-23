package com.hcmus.forumus_client.data.repository

import android.util.Log
import com.hcmus.forumus_client.data.model.ResetPasswordRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.data.model.UserRole
import com.hcmus.forumus_client.data.remote.NetworkService
import com.hcmus.forumus_client.service.EmailService
import com.hcmus.forumus_client.service.OTPService
import com.hcmus.forumus_client.utils.Resource
import com.hcmus.forumus_client.utils.ApiConstants
import kotlinx.coroutines.tasks.await

class AuthRepository (
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val otpService: OTPService = OTPService(firestore),
    private val emailService: EmailService = EmailService()
) {
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: UserRole
    ): Resource<User> {
        return try {
            // Create user in Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")

            // Send email verification
//            firebaseUser.sendEmailVerification().await()

            // Create user document in Firestore
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                fullName = fullName,
                role = role,
                emailVerified = false
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed")
        }
    }

    suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")

            // Get user data from Firestore
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found")

            Log.d("AuthRepository", "Login successful - User: ${user.email}, emailVerified: ${user.emailVerified}")
            Resource.Success(user)
        } catch (e: Exception) {
            Log.d("AuthRepository", "Login failed: ${e.message}")
            Resource.Error(e.message ?: "Login failed")
        }
    }

    suspend fun sendEmailVerification(): Resource<Boolean> {
        return try {
            firebaseAuth.currentUser?.sendEmailVerification()?.await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send verification email")
        }
    }

    /**
     * Generate and send OTP to user's email
     */
    suspend fun generateAndSendOTP(email: String): Resource<Boolean> {
        return try {
            // Generate OTP
            when (val otpResult = otpService.generateOTP(email)) {
                is Resource.Success -> {
                    // Send OTP via email
                    when (val emailResult = emailService.sendOTPEmail(email, otpResult.data!!)) {
                        is Resource.Success -> {
                            // Log the request for rate limiting
                            otpService.logOTPRequest(email)
                            Resource.Success(true)
                        }
                        is Resource.Error -> {
                            Resource.Error("Failed to send OTP email: ${emailResult.message}")
                        }
                        is Resource.Loading -> Resource.Loading()
                    }
                }
                is Resource.Error -> {
                    Resource.Error(otpResult.message ?: "Failed to generate OTP")
                }
                is Resource.Loading -> Resource.Loading()
            }
        } catch (e: Exception) {
            Resource.Error("Failed to send OTP: ${e.message}")
        }
    }
    
    /**
     * Verify OTP code entered by user
     */
    suspend fun verifyOTP(email: String, otpCode: String): Resource<Boolean> {
        return otpService.verifyOTP(email, otpCode)
    }
    
    /**
     * Resend OTP to user's email
     */
    suspend fun resendOTP(email: String): Resource<Boolean> {
        return generateAndSendOTP(email)
    }
    
    /**
     * Complete user verification after successful OTP verification
     */
    suspend fun completeEmailVerification(email: String): Resource<Boolean> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null && currentUser.email == email) {
                // Get current user document to check verification status
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java)
                Log.d("AuthRepository", user?.toString() ?: "User document is null")
                val wasAlreadyVerified = user?.emailVerified == true

                // Update user document in Firestore only if not already verified
                if (!wasAlreadyVerified) {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update("emailVerified", true)
                        .await()
                }

                // Only send welcome email for email verification (registration), never for login verification
                Log.d("AuthRepository", "completeEmailVerification - wasAlreadyVerified: $wasAlreadyVerified,")
                
                // NEVER send welcome email for login verification, only for email verification of new users
                if (!wasAlreadyVerified && user != null) {
                    Log.d("AuthRepository", "Sending welcome email to: $email")
                    emailService.sendWelcomeEmail(email, user.fullName)
                } else {
                    Log.d("AuthRepository", "NOT sending welcome email, wasAlreadyVerified: $wasAlreadyVerified")
                }
                
                Resource.Success(true)
            } else {
                Resource.Error("User not found or email mismatch")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to complete verification: ${e.message}")
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        return if (firebaseUser != null) {
            User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                fullName = firebaseUser.displayName ?: "",
                role = UserRole.STUDENT, // Default role; should fetch from Firestore in real app
            )
        } else null
    }

    fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    suspend fun checkAccountExists(email: String): Resource<Boolean> {
        return try {
            val userDoc = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            val exists = !userDoc.isEmpty
            Resource.Success(exists)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to check account existence")
        }
    }

    /**
     * Reset user password using server API with custom secret key
     */
    suspend fun resetPassword(email: String, newPassword: String, secretKey: String): Resource<Boolean> {
        return try {
            val request = ResetPasswordRequest(
                secretKey = secretKey,
                email = email,
                newPassword = newPassword
            )
            
            val response = NetworkService.apiService.resetPassword(request)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Resource.Success(true)
                } else {
                    Resource.Error(responseBody?.message ?: "Password reset failed")
                }
            } else {
                Resource.Error("Server error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    /**
     * Reset user password using server API with default secret key
     */
    suspend fun resetPassword(email: String, newPassword: String): Resource<Boolean> {
        return resetPassword(email, newPassword, ApiConstants.SECRET_KEY)
    }

}