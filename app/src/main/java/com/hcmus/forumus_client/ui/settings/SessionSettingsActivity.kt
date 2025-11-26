package com.hcmus.forumus_client.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.local.TokenManager
import com.hcmus.forumus_client.data.local.PreferencesManager
import com.hcmus.forumus_client.data.repository.AuthRepository
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SessionSettingsActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_settings)
        
        preferencesManager = PreferencesManager(this)
        tokenManager = TokenManager(this)
        authRepository = AuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            context = this
        )
        
        setupActionBar()
        setupPreferenceOptions()
    }
    
    private fun setupActionBar() {
        supportActionBar?.apply {
            title = "Session Settings"
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupPreferenceOptions() {
        // Auto-login toggle
        findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_auto_login).apply {
            isChecked = preferencesManager.isAutoLoginEnabled
            setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.isAutoLoginEnabled = isChecked
                Toast.makeText(this@SessionSettingsActivity, 
                    if (isChecked) "Auto-login enabled" else "Auto-login disabled", 
                    Toast.LENGTH_SHORT).show()
            }
        }
        
        // Remember me toggle
        findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_remember_me).apply {
            isChecked = preferencesManager.rememberMe
            setOnCheckedChangeListener { _, isChecked ->
                preferencesManager.rememberMe = isChecked
                Toast.makeText(this@SessionSettingsActivity, 
                    if (isChecked) "Will remember login email" else "Won't remember login email", 
                    Toast.LENGTH_SHORT).show()
            }
        }
        
        // Session timeout selector
        findViewById<android.widget.TextView>(R.id.tv_session_timeout).apply {
            text = "${preferencesManager.sessionTimeoutDays} days"
            setOnClickListener { showSessionTimeoutDialog() }
        }
        
        // Show current session info
        findViewById<android.widget.TextView>(R.id.tv_current_session_info).apply {
            val user = authRepository.getCurrentUserFromSession()
            val remainingTime = authRepository.getRemainingSessionTime()
            val daysRemaining = remainingTime / (24 * 60 * 60 * 1000)
            val hoursRemaining = (remainingTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
            
            text = "Current user: ${user?.email ?: "None"}\\n" +
                   "Session expires in: ${daysRemaining}d ${hoursRemaining}h"
        }
        
        // Logout button
        findViewById<android.widget.Button>(R.id.btn_logout).setOnClickListener {
            showLogoutConfirmation()
        }
        
        // Clear all sessions button
        findViewById<android.widget.Button>(R.id.btn_clear_sessions).setOnClickListener {
            showClearSessionsConfirmation()
        }
    }
    
    private fun showSessionTimeoutDialog() {
        val options = arrayOf("1 day", "3 days", "7 days", "14 days", "30 days")
        val values = arrayOf(1, 3, 7, 14, 30)
        val currentIndex = values.indexOf(preferencesManager.sessionTimeoutDays)
        
        AlertDialog.Builder(this)
            .setTitle("Session Timeout")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                preferencesManager.sessionTimeoutDays = values[which]
                findViewById<android.widget.TextView>(R.id.tv_session_timeout).text = "${values[which]} days"
                dialog.dismiss()
                Toast.makeText(this, "Session timeout set to ${values[which]} days", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? You will need to login again next time.")
            .setPositiveButton("Logout") { _, _ ->
                authRepository.logout()
                navigateToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showClearSessionsConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Sessions")
            .setMessage("This will clear all saved session data and you will need to login again.")
            .setPositiveButton("Clear") { _, _ ->
                tokenManager.clearSession()
                navigateToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}