package com.example.forumus.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.forumus.data.model.User
import com.example.forumus.data.model.UserRole
import com.example.forumus.service.EmailService
import com.example.forumus.service.OTPService
import com.example.forumus.utils.Resource
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
                isEmailVerified = false
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

            Resource.Success(user)
        } catch (e: Exception) {
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
                // Update user document in Firestore
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("isEmailVerified", true)
                    .await()
                
                // Send welcome email
                val userDoc = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    emailService.sendWelcomeEmail(email, user.fullName)
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
                profilePictureUrl = firebaseUser.photoUrl?.toString(),
                isEmailVerified = firebaseUser.isEmailVerified
            )
        } else null
    }

    fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null
}