package com.hcmus.forumus_client.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.local.TokenManager
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        authRepository = AuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            context = this
        )
        tokenManager = TokenManager(this)
        
        // TODO: Implement main app functionality

        logSessionInfo()
    }
    
    private fun logSessionInfo() {
        if (tokenManager.hasValidSession()) {
            val user = authRepository.getCurrentUserFromSession()
            val remainingTime = authRepository.getRemainingSessionTime()
            val hoursRemaining = remainingTime / (60 * 60 * 1000)
            
            Log.d("HomeActivity", "Current user: ${user?.email}")
            Log.d("HomeActivity", "Session remaining: ${hoursRemaining} hours")
        } else {
            Log.d("HomeActivity", "No saved session - user didn't check Remember Me")
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_session_info -> {
                showSessionInfo()
                true
            }
            R.id.action_settings -> {
                openSessionSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun logout() {
        authRepository.logout()
        redirectToLogin()
    }
    
    private fun showSessionInfo() {
        if (tokenManager.hasValidSession()) {
            val user = authRepository.getCurrentUserFromSession()
            val remainingTime = authRepository.getRemainingSessionTime()
            val daysRemaining = remainingTime / (24 * 60 * 60 * 1000)
            val hoursRemaining = (remainingTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
            
            val message = "Welcome ${user?.fullName}!\\nSession expires in: ${daysRemaining}d ${hoursRemaining}h"
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Session Info")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        } else {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Session Info")
                .setMessage("No saved session\\n\\nYou didn't check 'Remember Me' during login, so you'll need to login again when you restart the app.")
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    private fun openSessionSettings() {
        val intent = Intent(this, com.hcmus.forumus_client.ui.settings.SessionSettingsActivity::class.java)
        startActivity(intent)
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        // No session validity check - users can access app regardless of Remember Me choice
        // Session validation only happens at app startup in SplashActivity
    }
}