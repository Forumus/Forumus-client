package com.hcmus.forumus_client.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivitySplashBinding
import com.hcmus.forumus_client.data.local.TokenManager
import com.hcmus.forumus_client.data.local.PreferencesManager
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.hcmus.forumus_client.ui.main.MainActivity
import com.hcmus.forumus_client.ui.onboarding.welcome.WelcomeActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferenceManager: PreferencesManager
    private lateinit var tokenManager: TokenManager
    private val splashTimeOut: Long = 1500 // 1.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme preference BEFORE setting content view
        val preferencesManager = PreferencesManager(this)
        if (preferencesManager.isDarkModeEnabled) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            )
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        
        super.onCreate(savedInstanceState)
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        
        this.preferenceManager = preferencesManager  // Reuse the instance
        tokenManager = TokenManager(this)
        
        // Navigate after splash timeout
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashTimeOut)
    }
    
    private fun navigateToNextScreen() {
        val nextActivity = when {
            preferenceManager.isFirstTime -> {
                Log.d("SplashActivity", "First time user - showing onboarding")
                WelcomeActivity::class.java
            }
            preferenceManager.isAutoLoginEnabled &&
            preferenceManager.rememberMe &&
            tokenManager.hasValidSession() -> {
                val remainingTime = tokenManager.getRemainingSessionTime()
                val daysRemaining = remainingTime / (24 * 60 * 60 * 1000)
                Log.d("SplashActivity", "Valid session found - remember me enabled, $daysRemaining days remaining")
                MainActivity::class.java
            }
            else -> {
                Log.d("SplashActivity", "No valid session, remember me disabled, or auto-login disabled - showing login")
                LoginActivity::class.java
            }
        }
        
        val intent = Intent(this, nextActivity)
        // Clear task stack when navigating to home from valid session
        if (nextActivity == MainActivity::class.java) {
            intent.putExtras(getIntent())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}