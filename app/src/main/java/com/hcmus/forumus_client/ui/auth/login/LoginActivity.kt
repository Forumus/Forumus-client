package com.hcmus.forumus_client.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.hcmus.forumus_client.databinding.ActivityLoginBinding
import com.hcmus.forumus_client.ui.auth.register.RegisterActivity
import com.hcmus.forumus_client.utils.Resource
import android.text.SpannableString
import androidx.core.content.ContextCompat
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.method.LinkMovementMethod
import android.graphics.Typeface
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.ui.auth.forgotPassword.ForgotPasswordActivity
import com.hcmus.forumus_client.ui.auth.verification.VerificationActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    companion object {
        const val VERIFICATION_TYPE = "verification_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force light theme regardless of device night mode.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHintBehavior()

        setupClickListeners()
        setupTextChangeListeners()
        setupRegisterText()
        setupForgotPasswordText()
        observeLoginState()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            // Clear previous errors
            binding.tilEmail.isErrorEnabled = false
            binding.tilPassword.isErrorEnabled = false
            
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
                binding.tilPassword.error = "Please enter your password"
                return@setOnClickListener
            }
            
            viewModel.login(email, password)
        }
    }

    private fun setupHintBehavior() {
        // Clear hint on focus, restore if empty on blur
        applyHintFocusBehavior(binding.etEmail, binding.tilEmail, getString(R.string.enter_your_email))
        applyHintFocusBehavior(binding.etPassword, binding.tilPassword, getString(R.string.password))
    }

    private fun applyHintFocusBehavior(editText: com.google.android.material.textfield.TextInputEditText,
                                       layout: com.google.android.material.textfield.TextInputLayout,
                                       originalHint: String) {
        // Ensure TextInputLayout itself has no hint to prevent floating label
        layout.hint = null
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Remove hint when focusing
                if (editText.hint != null) editText.hint = ""
            } else {
                // Restore only if no text entered
                if (editText.text.isNullOrEmpty()) {
                    editText.hint = originalHint
                }
            }
        }
        // Initial restore if empty (after configuration changes)
        if (editText.text.isNullOrEmpty()) editText.hint = originalHint
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
    }

    private fun observeLoginState() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    val user = resource.data
                    val intent = Intent(this, VerificationActivity::class.java)
                    intent.putExtra(VerificationActivity.EXTRA_EMAIL, user?.email)

                    if (user?.emailVerified == false) {
                        Log.d("LoginActivity", "User not verified, setting email_verification type")
                        intent.putExtra(VERIFICATION_TYPE, "email_verification")
                    } else {
                        Log.d("LoginActivity", "User already verified, setting login_verification type")
                        intent.putExtra(VERIFICATION_TYPE, "login_verification")
                    }
                    startActivity(intent)

                }
                is Resource.Error -> {
                    showLoading(false)
                    val errorMessage = resource.message ?: "Login failed"
                    
                    // Show specific field errors based on error message
                    when {
                        errorMessage.contains("credential", ignoreCase = true) -> {
                            binding.tilPassword.isErrorEnabled = true
                            binding.tilPassword.error = "Invalid password"
                        }
                        errorMessage.contains("email", ignoreCase = true) || 
                        errorMessage.contains("user", ignoreCase = true) -> {
                            binding.tilEmail.isErrorEnabled = true
                            binding.tilEmail.error = "No account found with this email"
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
        binding.progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "" else getString(R.string.login)
    }

    private fun setupRegisterText() {
        val fullText = "${getString(R.string.new_member)} ${getString(R.string.register_now)}"
        val clickableText = getString(R.string.register_now)
        
        createClickableText(
            textView = binding.tvRegisterLink,
            fullText = fullText,
            clickableText = clickableText
        ) {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }
    }

    private fun setupForgotPasswordText() {
        val fullText = getString(R.string.forgot_password)
        val clickableText = getString(R.string.forgot_password)

        createClickableText(
            textView = binding.tvForgotPassword,
            fullText = fullText,
            clickableText = clickableText
        ) {
            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
        }
    }

    private fun createClickableText(
        textView: android.widget.TextView,
        fullText: String,
        clickableText: String,
        onClick: () -> Unit
    ) {
        val spannableString = SpannableString(fullText)
        val linkColor = ContextCompat.getColor(this, R.color.link_color)

        val clickableStart = fullText.indexOf(clickableText)
        val clickableEnd = clickableStart + clickableText.length

        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                onClick()
            }
        }, clickableStart, clickableEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spannableString.setSpan(
            ForegroundColorSpan(linkColor),
            clickableStart, clickableEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            clickableStart, clickableEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}