package com.hcmus.forumus_client.ui.auth.verification

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.KeyEvent
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityVerificationBinding
import com.hcmus.forumus_client.ui.auth.success.SuccessActivity
import com.hcmus.forumus_client.ui.MainActivity
import com.hcmus.forumus_client.ui.auth.resetPassword.ResetPasswordActivity

class VerificationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVerificationBinding
    private lateinit var viewModel: VerificationViewModel
    private lateinit var otpEditTexts: Array<EditText>
    private var countDownTimer: CountDownTimer? = null
    private var userEmail: String = "longto@discord.com" // Default or passed from intent
    private var verificationType: String = "registration"
    
    companion object {
        const val EXTRA_EMAIL = "extra_email"
        private const val COUNTDOWN_TIME = 5 * 60 * 1000L // 5 minutes in milliseconds
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[VerificationViewModel::class.java]
        
        // Get email from intent if available
        userEmail = intent.getStringExtra(EXTRA_EMAIL) ?: userEmail

        // Get verification type from intent if needed
        verificationType = intent.getStringExtra("verification_type") ?: "registration"
        
        setupUI()
        setupOtpInputs()
        observeViewModel()
        startCountdown()
        
        // Send initial OTP when screen opens
        viewModel.sendInitialOTP(userEmail)
    }
    
    private fun setupUI() {
        // Set up description with email
        setupDescriptionText()
        
        // Set up click listeners
        binding.btnVerify.setOnClickListener {
            verifyOtp()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.tvDidntReceive.setOnClickListener {
            resendCode()
        }
    }
    
    private fun setupDescriptionText() {
        val hiddenUserEmail = if (userEmail.length > 4) {
            val atIndex = userEmail.indexOf('@')
            if (atIndex > 2) {
                userEmail.substring(0, 2) + "****" + userEmail.substring(atIndex)
            } else {
                "****" + userEmail.substring(atIndex)
            }
        } else {
            userEmail
        }
        val descriptionText = getString(R.string.verification_description, hiddenUserEmail)
        val spannableString = SpannableString(descriptionText)
        
        // Find email in the text and make it bold and colored
        val emailStart = descriptionText.indexOf(hiddenUserEmail)
        if (emailStart != -1) {
            val emailEnd = emailStart + hiddenUserEmail.length
            
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                emailStart, emailEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_primary)),
                emailStart, emailEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        binding.tvDescription.text = spannableString
    }
    
    private fun setupOtpInputs() {
        otpEditTexts = arrayOf(
            binding.etOtp1,
            binding.etOtp2,
            binding.etOtp3,
            binding.etOtp4,
            binding.etOtp5,
            binding.etOtp6
        )
        
        // Set up text watchers and key listeners for each OTP input
        otpEditTexts.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        // Move to next field if not the last one
                        if (index < otpEditTexts.size - 1) {
                            otpEditTexts[index + 1].requestFocus()
                        }
                        
                        // Check if all fields are filled
                        if (areAllFieldsFilled()) {
                            verifyOtp()
                        }
                    }
                }
            })
            
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        // Move to previous field if current is empty and backspace is pressed
                        otpEditTexts[index - 1].requestFocus()
                        otpEditTexts[index - 1].setText("")
                    }
                }
                false
            }
        }
        
        // Focus on first input
        otpEditTexts[0].requestFocus()
    }
    
    private fun observeViewModel() {
        viewModel.verificationResult.observe(this) { result ->
            if (result.isSuccess) {
                // Handle successful verification
                when (verificationType) {
                    "email_verification", "registration" -> {
                        val intent = Intent(this, SuccessActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    "login_verification" -> {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    "forgot_password" -> {
                        val intent = Intent(this, ResetPasswordActivity::class.java)
                        intent.putExtra("user_email", userEmail)
                        startActivity(intent)
                        finish()
                    }
                    else -> finish()
                }
            } else {
                // Handle verification error
                clearOtpInputs()
                android.widget.Toast.makeText(
                    this, 
                    result.exceptionOrNull()?.message ?: "Verification failed. Please try again.", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.resendResult.observe(this) { result ->
            if (result.isSuccess) {
                // Restart countdown
                startCountdown()
                android.widget.Toast.makeText(
                    this, 
                    "Verification code has been resent to your email.", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    this, 
                    result.exceptionOrNull()?.message ?: "Failed to resend code. Please try again.", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarVerify.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnVerify.isEnabled = !isLoading
            binding.tvDidntReceive.isEnabled = !isLoading
            binding.btnVerify.text = if (isLoading) "" else getString(R.string.verify)
        }
    }
    private fun verifyOtp() {
        val otp = getOtpFromInputs()
        if (otp.length == 6) {
            viewModel.verifyOtp(userEmail, otp)
        }
    }
    
    private fun resendCode() {
        viewModel.resendOtp(userEmail)
    }
    
    private fun getOtpFromInputs(): String {
        return otpEditTexts.joinToString("") { it.text.toString() }
    }
    
    private fun areAllFieldsFilled(): Boolean {
        return otpEditTexts.all { it.text.isNotEmpty() }
    }
    
    private fun clearOtpInputs() {
        otpEditTexts.forEach { 
            it.setText("")
        }
        otpEditTexts[0].requestFocus()
    }
    
    private fun startCountdown() {
        countDownTimer?.cancel()
        
        countDownTimer = object : CountDownTimer(COUNTDOWN_TIME, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val timeText = String.format("%02d:%02d", seconds / 60, seconds % 60)
                binding.tvResendTimer.text = getString(R.string.request_new_code_timer, timeText + "s")
            }
            
            override fun onFinish() {
                binding.tvResendTimer.text = "Request new code now"
                binding.tvDidntReceive.isEnabled = true
            }
        }
        
        binding.tvDidntReceive.isEnabled = false
        countDownTimer?.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}