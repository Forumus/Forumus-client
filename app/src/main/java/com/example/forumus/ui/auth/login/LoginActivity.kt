package com.example.forumus.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.forumus.R
import com.example.forumus.databinding.ActivityLoginBinding
import com.example.forumus.ui.auth.register.RegisterActivity

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Set up register link text
        setupRegisterText()
        
        // Set up click listeners
        binding.btnLogin.setOnClickListener {
            loginUser()
        }
        
        binding.tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }
    }
    
    private fun setupRegisterText() {
        val fullText = "${getString(R.string.new_member)} ${getString(R.string.register_now)}"
        val spannableString = SpannableString(fullText)
        
        val linkColor = ContextCompat.getColor(this, R.color.link_color)
        
        // Find and make "Register now" clickable
        val registerStart = fullText.indexOf(getString(R.string.register_now))
        val registerEnd = registerStart + getString(R.string.register_now).length
        
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigateToRegister()
            }
        }, registerStart, registerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        spannableString.setSpan(
            ForegroundColorSpan(linkColor),
            registerStart, registerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            registerStart, registerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        binding.tvRegisterLink.text = spannableString
        binding.tvRegisterLink.movementMethod = LinkMovementMethod.getInstance()
    }
    
    private fun observeViewModel() {
        // Observe login result
        viewModel.loginResult.observe(this) { result ->
            if (result.isSuccess) {
                // Handle successful login
                // Navigate to main activity or dashboard
            } else {
                // Handle login error
                // Show error message
            }
        }
    }
    
    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val rememberMe = binding.cbRemember.isChecked
        
        if (validateInput(email, password)) {
            viewModel.login(email, password, rememberMe)
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }
        
        return isValid
    }
    
    private fun handleForgotPassword() {
        // TODO: Implement forgot password functionality
        // Navigate to forgot password screen or show dialog
    }
    
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        finish()
    }
}