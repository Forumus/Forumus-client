package com.hcmus.forumus_client.ui.auth.verification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class VerificationViewModel : ViewModel() {
    
    private val authRepository = AuthRepository(
        FirebaseAuth.getInstance(), 
        FirebaseFirestore.getInstance()
    )
    
    private val _verificationResult = MutableLiveData<Result<Boolean>>()
    val verificationResult: LiveData<Result<Boolean>> = _verificationResult
    
    private val _resendResult = MutableLiveData<Result<Boolean>>()
    val resendResult: LiveData<Result<Boolean>> = _resendResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Send initial OTP when verification screen is opened
     */
    fun sendInitialOTP(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            when (val result = authRepository.generateAndSendOTP(email)) {
                is Resource.Success -> {
                    _resendResult.value = Result.success(true)
                }
                is Resource.Error -> {
                    _resendResult.value = Result.failure(Exception(result.message))
                }
                is Resource.Loading -> {
                    // Continue loading
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Verify OTP entered by user
     */
    fun verifyOtp(email: String, otp: String) {
        // Validate OTP format
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _verificationResult.value = Result.failure(Exception("Please enter a valid 6-digit OTP"))
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            when (val result = authRepository.verifyOTP(email, otp)) {
                is Resource.Success -> {
                    if (result.data == true) {
                        // Complete verification process
                        completeVerification(email)
                    } else {
                        _verificationResult.value = Result.failure(Exception("OTP verification failed"))
                    }
                }
                is Resource.Error -> {
                    _verificationResult.value = Result.failure(Exception(result.message))
                }
                is Resource.Loading -> {
                    // Continue loading
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Complete the verification process
     */
    private suspend fun completeVerification(email: String) {
        when (val result = authRepository.completeEmailVerification(email)) {
            is Resource.Success -> {
                _verificationResult.value = Result.success(true)
            }
            is Resource.Error -> {
                // OTP was correct but verification completion failed
                // Still show success to user
                _verificationResult.value = Result.success(true)
            }
            is Resource.Loading -> {
                // Continue loading
            }
        }
    }
    
    /**
     * Resend OTP to user's email
     */
    fun resendOtp(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            when (val result = authRepository.resendOTP(email)) {
                is Resource.Success -> {
                    _resendResult.value = Result.success(true)
                }
                is Resource.Error -> {
                    _resendResult.value = Result.failure(Exception(result.message))
                }
                is Resource.Loading -> {
                    // Continue loading
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Validate OTP format (6 digits only)
     */
    fun validateOtpFormat(otp: String): Boolean {
        return otp.length == 6 && otp.all { it.isDigit() }
    }
}