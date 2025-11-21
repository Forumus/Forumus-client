package com.hcmus.forumus_client.ui.auth.resetPassword

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.databinding.ActivityResetPasswordBinding
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.hcmus.forumus_client.utils.ValidationUtils
import com.hcmus.forumus_client.utils.Resource
import com.hcmus.forumus_client.R

class ResetPasswordActivity : AppCompatActivity() {
    private val viewModel: ResetPasswordViewModel by viewModels()
    private lateinit var binding: ActivityResetPasswordBinding
    private var userEmail: String = "longto@discord.com" // Default or passed from intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get email from intent if available
        userEmail = intent.getStringExtra("user_email") ?: userEmail

        setupClickListeners()
        observeResetPasswordState()
    }

    private fun setupClickListeners() {
        binding.btnConfirm.setOnClickListener {
            val newPassword = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (newPassword.isBlank()) {
                binding.etPassword.error = "Please enter a new password"
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidPassword(newPassword)) {
                binding.etPassword.error = "Password must be at least 8 characters, include uppercase, lowercase, number, and special character"
                return@setOnClickListener
            }

            viewModel.resetPassword(userEmail, newPassword)
        }
    }

    private fun observeResetPasswordState() {
        viewModel.resetPasswordState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnConfirm.isEnabled = !isLoading
        binding.btnConfirm.text = if (isLoading) "" else getString(R.string.confirm)
        binding.btnCancel.isEnabled = !isLoading
    }
}