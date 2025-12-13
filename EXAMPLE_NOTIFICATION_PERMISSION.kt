package com.hcmus.forumus_client.example

/**
 * EXAMPLE: How to add notification permission request to your Activity
 * 
 * Add this code to your LoginActivity, HomeActivity, or SplashActivity
 * after successful authentication.
 */

// In your Activity class:

/*
import com.hcmus.forumus_client.utils.NotificationPermissionHelper

class HomeActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Request notification permission after user is logged in
        requestNotificationPermission()
    }
    
    private fun requestNotificationPermission() {
        NotificationPermissionHelper.requestPermissionIfNeeded(this) { granted ->
            if (granted) {
                Log.d("HomeActivity", "Notification permission granted - FCM ready")
                // You can show a success message if you want
            } else {
                Log.d("HomeActivity", "Notification permission denied")
                // Optionally show a message explaining why notifications are useful
                Toast.makeText(
                    this,
                    "Enable notifications to receive chat messages",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        NotificationPermissionHelper.handlePermissionResult(requestCode, grantResults)
    }
}
*/

/*
 * ALTERNATIVE: If you're using AndroidX Activity Result API
 * (Recommended for newer apps)
 */

/*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class HomeActivity : AppCompatActivity() {
    
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("HomeActivity", "Notification permission granted")
            // Initialize FCM
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                Log.d("FCM", "Token: $token")
            }
        } else {
            Log.d("HomeActivity", "Notification permission denied")
            Toast.makeText(
                this,
                "You won't receive chat notifications",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        // Request permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Request permission
                    requestNotificationPermission.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }
    }
}
*/
