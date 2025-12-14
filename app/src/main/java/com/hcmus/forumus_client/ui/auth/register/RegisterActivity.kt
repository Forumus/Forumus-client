package com.hcmus.forumus_client.ui.auth.register

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.model.UserRole
import com.hcmus.forumus_client.databinding.ActivityRegisterBinding
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.hcmus.forumus_client.ui.auth.verification.VerificationActivity
import com.hcmus.forumus_client.utils.Resource

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupTextChangeListeners()
        setupLoginText()
        setupHintBehavior()
        observeRegisterState()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val isTermsAccepted = binding.cbTerms.isChecked

            // Clear previous errors
            binding.tilFullName.isErrorEnabled = false
            binding.tilEmail.isErrorEnabled = false
            binding.tilPassword.isErrorEnabled = false
            binding.tilConfirmPassword.isErrorEnabled = false

            // Validate full name
            if (fullName.isBlank()) {
                binding.tilFullName.isErrorEnabled = true
                binding.tilFullName.error = "Please enter your full name"
                return@setOnClickListener
            }

            // Validate email
            if (email.isBlank()) {
                binding.tilEmail.isErrorEnabled = true
                binding.tilEmail.error = "Please enter your email"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.isErrorEnabled = true
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            // Validate password
            if (password.isBlank()) {
                binding.tilPassword.isErrorEnabled = true
                binding.tilPassword.error = "Please enter a password"
                return@setOnClickListener
            }

            if (password.length < 8) {
                binding.tilPassword.isErrorEnabled = true
                binding.tilPassword.error = "Password must be at least 8 characters"
                return@setOnClickListener
            }

            // Validate confirm password
            if (confirmPassword.isBlank()) {
                binding.tilConfirmPassword.isErrorEnabled = true
                binding.tilConfirmPassword.error = "Please confirm your password"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.tilConfirmPassword.isErrorEnabled = true
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val role = when (binding.rgRole.checkedRadioButtonId) {
                binding.rbTeacher.id -> UserRole.TEACHER
                else -> UserRole.STUDENT
            }

            if (!isTermsAccepted) {
                Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(fullName, email, password, confirmPassword, role)
        }
    }
    
    private fun setupTextChangeListeners() {
        binding.etFullName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.tilFullName.error != null) {
                    binding.tilFullName.error = null
                    binding.tilFullName.isErrorEnabled = false
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
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

    private fun observeRegisterState() {
        viewModel.registerState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }

                is Resource.Success -> {
                    showLoading(false)
                    val email = binding.etEmail.text.toString().trim()
                    val intent = Intent(this, VerificationActivity::class.java)
                    intent.putExtra(VerificationActivity.EXTRA_EMAIL, email)
                    intent.putExtra("verification_type", "email_verification")
                    startActivity(intent)
                    finish() // Close registration activity
                }

                is Resource.Error -> {
                    showLoading(false)
                    val errorMessage = resource.message ?: "Registration failed"
                    
                    // Show specific field errors based on error message
                    when {
                        errorMessage.contains("email", ignoreCase = true) && 
                        errorMessage.contains("already", ignoreCase = true) -> {
                            binding.tilEmail.isErrorEnabled = true
                            binding.tilEmail.error = "An account with this email already exists"
                        }
                        errorMessage.contains("email", ignoreCase = true) -> {
                            binding.tilEmail.isErrorEnabled = true
                            binding.tilEmail.error = "Invalid email address"
                        }
                        errorMessage.contains("password", ignoreCase = true) -> {
                            binding.tilPassword.isErrorEnabled = true
                            binding.tilPassword.error = "Password requirements not met"
                        }
                        else -> {
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarRegister.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "" else getString(R.string.register)
    }

    private fun setupLoginText() {
        val fullText = "${getString(R.string.already_member)} ${getString(R.string.login_now)}"
        val spannableString = SpannableString(fullText)

        val linkColor = ContextCompat.getColor(this, R.color.link_color)

        val loginStart = fullText.indexOf(getString(R.string.login_now))
        val loginEnd = loginStart + getString(R.string.login_now).length

        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            }
        }, loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spannableString.setSpan(
            ForegroundColorSpan(linkColor),
            loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvLoginLink.text = spannableString
        binding.tvLoginLink.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupHintBehavior() {
        applyHintFocusBehavior(binding.etFullName, binding.tilFullName, getString(R.string.full_name))
        applyHintFocusBehavior(binding.etEmail, binding.tilEmail, getString(R.string.valid_email))
        applyHintFocusBehavior(binding.etPassword, binding.tilPassword, getString(R.string.strong_password))
        applyHintFocusBehavior(binding.etConfirmPassword, binding.tilConfirmPassword, getString(R.string.confirm_password))
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
            } else {
                if (editText.text.isNullOrEmpty()) editText.hint = originalHint
            }
        }
        if (editText.text.isNullOrEmpty()) editText.hint = originalHint
    }

}