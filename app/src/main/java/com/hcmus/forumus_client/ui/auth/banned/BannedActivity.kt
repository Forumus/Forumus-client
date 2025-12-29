package com.hcmus.forumus_client.ui.auth.banned

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.hcmus.forumus_client.data.local.TokenManager
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.databinding.ActivityBannedBinding
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.hcmus.forumus_client.ui.main.MainActivity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

/**
 * Activity displayed when a user is banned. Shows a countdown timer until the ban expires. Blocks
 * all user interaction and prevents access to the app while banned.
 */
class BannedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBannedBinding
    private lateinit var userRepository: UserRepository
    private lateinit var tokenManager: TokenManager

    private val handler = Handler(Looper.getMainLooper())
    private var blacklistedUntil: Long = 0

    // Runnable to update countdown every second
    private val countdownRunnable =
            object : Runnable {
                override fun run() {
                    updateCountdown()
                    handler.postDelayed(this, 1000) // Update every second
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBannedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository()
        tokenManager = TokenManager(this)

        // Get ban expiration timestamp from intent
        blacklistedUntil = intent.getLongExtra(EXTRA_BLACKLISTED_UNTIL, 0)

        if (blacklistedUntil == 0L) {
            // If no valid timestamp, check user status from repository
            checkUserStatus()
        } else {
            // Display ban information
            displayBanInfo()
            startCountdown()
        }

        // Prevent back button navigation
        onBackPressedDispatcher.addCallback(this) {
            // Do nothing - user cannot go back
        }
    }

    private fun checkUserStatus() {
        lifecycleScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                if (currentUser == null) {
                    Log.e("BannedActivity", "No current user found")
                    logout()
                    return@launch
                }

                blacklistedUntil = currentUser.blacklistedUntil ?: 0

                if (blacklistedUntil == 0L || System.currentTimeMillis() >= blacklistedUntil) {
                    // Ban has expired or doesn't exist
                    Log.d("BannedActivity", "Ban expired or not present, navigating to main")
                    navigateToMain()
                } else {
                    displayBanInfo()
                    startCountdown()
                }
            } catch (e: Exception) {
                Log.e("BannedActivity", "Error checking user status", e)
                binding.tvErrorMessage.text = "Error checking ban status. Please try again later."
                binding.tvErrorMessage.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun displayBanInfo() {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        val expirationDate = dateFormat.format(Date(blacklistedUntil))

        binding.tvBanExpiration.text = "Ban expires on: $expirationDate"
    }

    private fun startCountdown() {
        handler.post(countdownRunnable)
    }

    private fun updateCountdown() {
        val currentTime = System.currentTimeMillis()
        val remainingTime = blacklistedUntil - currentTime

        if (remainingTime <= 0) {
            // Ban has expired
            handler.removeCallbacks(countdownRunnable)
            binding.tvCountdown.text = "00:00:00:00"
            binding.tvBanMessage.text = "Your ban has expired. Redirecting..."

            // Wait 2 seconds then navigate to main
            handler.postDelayed({ navigateToMain() }, 2000)
        } else {
            // Calculate time components
            val days = TimeUnit.MILLISECONDS.toDays(remainingTime)
            val hours = TimeUnit.MILLISECONDS.toHours(remainingTime) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60

            // Format countdown display
            val countdownText =
                    String.format(
                            Locale.getDefault(),
                            "%02d:%02d:%02d:%02d",
                            days,
                            hours,
                            minutes,
                            seconds
                    )

            binding.tvCountdown.text = countdownText
        }
    }

    private fun navigateToMain() {
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.flags =
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun logout() {
        // Clear session and navigate to login
        tokenManager.clearSession()
        FirebaseAuth.getInstance().signOut()

        // Navigate to login activity
        val intent = android.content.Intent(this, LoginActivity::class.java)
        intent.flags =
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(countdownRunnable)
    }

    companion object {
        const val EXTRA_BLACKLISTED_UNTIL = "blacklisted_until"
    }
}
