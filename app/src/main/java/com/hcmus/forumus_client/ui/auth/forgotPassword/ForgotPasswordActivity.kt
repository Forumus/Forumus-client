package com.hcmus.forumus_client.ui.auth.forgotPassword

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.databinding.ActivityForgotPasswordBinding
import com.hcmus.forumus_client.ui.auth.verification.VerificationActivity
import com.hcmus.forumus_client.utils.ValidationUtils
import com.hcmus.forumus_client.utils.Resource
import com.hcmus.forumus_client.R

class ForgotPasswordActivity : AppCompatActivity() {
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private lateinit var binding: ActivityForgotPasswordBinding
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupTextChangeListeners()
        observeAccountExistsState()
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            userEmail = binding.etEmail.text.toString().trim()

            // Clear previous errors
            binding.tilEmail.isErrorEnabled = false
            
            if (userEmail.isBlank()) {
                binding.tilEmail.isErrorEnabled = true
                binding.tilEmail.error = "Please enter your email"
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidEmail(userEmail)) {
                binding.tilEmail.isErrorEnabled = true
                binding.tilEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }

            viewModel.checkAccountExists(userEmail)

        }
    }
    
    private fun setupTextChangeListeners() {
        binding.etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.tilEmail.error != null) {
                    binding.tilEmail.error = null
                    binding.tilEmail.isErrorEnabled = false
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun observeAccountExistsState() {
        viewModel.accountExistsState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    val exists = resource.data ?: false

                    if (exists) {
                        startActivity(Intent(this, VerificationActivity::class.java).apply {
                            putExtra("extra_email", userEmail)
                            putExtra("verification_type", "forgot_password")
                        })
                    } else {
                        binding.tilEmail.isErrorEnabled = true
                        binding.tilEmail.error = "No account found with this email"
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    binding.tilEmail.isErrorEnabled = true
                    binding.tilEmail.error = resource.message ?: "An error occurred"
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarSubmit.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
        binding.btnSubmit.text = if (isLoading) "" else getString(R.string.submit)
    }
}