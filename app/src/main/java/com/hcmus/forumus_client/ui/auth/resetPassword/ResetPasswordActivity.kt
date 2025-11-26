package com.hcmus.forumus_client.ui.auth.resetPassword

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get email from intent if available
        userEmail = intent.getStringExtra("user_email") ?: userEmail

        setupClickListeners()
        setupTextChangeListeners()
        setupHintBehavior()
        observeResetPasswordState()
    }

    private fun setupClickListeners() {
        binding.btnConfirm.setOnClickListener {
            val newPassword = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            // Clear previous errors
            binding.tilPassword.isErrorEnabled = false
            binding.tilConfirmPassword.isErrorEnabled = false
            
            if (newPassword.isBlank()) {
                binding.tilPassword.isErrorEnabled = true
                binding.tilPassword.error = "Please enter a new password"
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                binding.tilConfirmPassword.isErrorEnabled = true
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidPassword(newPassword)) {
                binding.tilPassword.isErrorEnabled = true
                binding.tilPassword.error = "Password must be at least 8 characters, include uppercase, lowercase, number, and special character"
                return@setOnClickListener
            }

            viewModel.resetPassword(userEmail, newPassword)
        }
    }
    
    private fun setupTextChangeListeners() {
        binding.etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.tilPassword.error != null) {
                    binding.tilPassword.error = null
                    binding.tilPassword.isErrorEnabled = false
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        binding.etConfirmPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.tilConfirmPassword.error != null) {
                    binding.tilConfirmPassword.error = null
                    binding.tilConfirmPassword.isErrorEnabled = false
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupHintBehavior() {
        applyHintFocusBehavior(binding.etPassword, binding.tilPassword, getString(R.string.strong_password))
        applyHintFocusBehavior(binding.etConfirmPassword, binding.tilConfirmPassword, getString(R.string.retype_password))
    }

    private fun applyHintFocusBehavior(
        editText: com.google.android.material.textfield.TextInputEditText,
        layout: com.google.android.material.textfield.TextInputLayout,
        originalHint: String
    ) {
        layout.hint = null
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (editText.hint != null) editText.hint = ""
            } else if (editText.text.isNullOrEmpty()) {
                editText.hint = originalHint
            }
        }
        if (editText.text.isNullOrEmpty()) editText.hint = originalHint
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