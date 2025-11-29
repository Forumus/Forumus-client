package com.hcmus.forumus_client.ui.auth.success

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.databinding.ActivitySuccessBinding
import com.hcmus.forumus_client.data.local.TokenManager
import com.hcmus.forumus_client.data.local.PreferencesManager
import com.hcmus.forumus_client.ui.MainActivity
import com.google.firebase.auth.FirebaseAuth

class SuccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure session is saved after successful verification
        ensureSessionIsSaved()

        // Auto-navigate to Home after short delay to reduce confusion
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToHome()
        }, 1800)

        binding.btnFinish.setOnClickListener {
            navigateToHome()
        }
    }
    
    private fun ensureSessionIsSaved() {
        val tokenManager = TokenManager(this)
        val preferencesManager = PreferencesManager(this)
        val firebaseAuth = FirebaseAuth.getInstance()
        
        // Only save session if remember me is enabled and we don't already have a valid session
        if (preferencesManager.rememberMe && !tokenManager.hasValidSession()) {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                Log.d("SuccessActivity", "Remember Me was checked - saving session for auto-login: ${currentUser.email}")
                
                // Save session for current Firebase user for auto-login
                val sessionDuration = preferencesManager.getSessionTimeoutMs()
                tokenManager.saveUserSession(
                    userId = currentUser.uid,
                    email = currentUser.email ?: "",
                    fullName = currentUser.displayName ?: "",
                    role = "STUDENT", // Default role
                    emailVerified = true,
                    sessionDurationMs = sessionDuration
                )
                
                Log.d("SuccessActivity", "Auto-login session saved, expires in ${sessionDuration / (24 * 60 * 60 * 1000)} days")
            }
        } else {
            Log.d("SuccessActivity", "Remember Me not checked - user will need to login manually on next app start")
        }
    }
    
    private fun navigateToHome() {
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
