package com.hcmus.forumus_client.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivitySplashBinding
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.hcmus.forumus_client.ui.onboarding.welcome.WelcomeActivity
import com.hcmus.forumus_client.utils.PreferenceManager

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferenceManager: PreferenceManager
    private val splashTimeOut: Long = 1500 // 1.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        
        preferenceManager = PreferenceManager(this)
        
        // Navigate after splash timeout
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashTimeOut)
    }    private fun navigateToNextScreen() {
        val nextActivity = if (preferenceManager.isFirstTime) {
            WelcomeActivity::class.java
        } else {
            LoginActivity::class.java
        }
        
        val intent = Intent(this, nextActivity)
        startActivity(intent)
        finish()
    }
}