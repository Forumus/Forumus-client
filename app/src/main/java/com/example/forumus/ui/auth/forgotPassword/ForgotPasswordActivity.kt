package com.example.forumus.ui.auth.forgotPassword

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.forumus.databinding.ActivityForgotPasswordBinding
import com.example.forumus.ui.auth.verification.VerificationActivity
import com.example.forumus.utils.ValidationUtils
import com.example.forumus.utils.Resource
import com.example.forumus.R

class ForgotPasswordActivity : AppCompatActivity() {
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private lateinit var binding: ActivityForgotPasswordBinding
    private var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeAccountExistsState()
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            userEmail = binding.etEmail.text.toString().trim()

            if (userEmail.isEmpty()) {
                binding.etEmail.error = "Please enter your email"
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidEmail(userEmail)) {
                binding.etEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }

            viewModel.checkAccountExists(userEmail)

        }
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
                        binding.etEmail.error = "No account found with this email"
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    binding.etEmail.error = resource.message ?: "An error occurred"
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