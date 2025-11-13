package com.example.forumus.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.forumus.databinding.ActivityLoginBinding
import com.example.forumus.ui.auth.register.RegisterActivity
import com.example.forumus.ui.main.MainActivity
import com.example.forumus.utils.Resource
import android.text.SpannableString
import androidx.core.content.ContextCompat
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.method.LinkMovementMethod
import android.graphics.Typeface
import com.example.forumus.R
import com.example.forumus.ui.auth.verification.VerificationActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupRegisterText()
        observeLoginState()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            viewModel.login(email, password)
        }
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

                    if (user?.isEmailVerified == false) {
                        startActivity(Intent(this, VerificationActivity::class.java))
                    } else {
                        // Navigate to main screen
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
//        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

//    private fun showEmailNotVerifiedDialog() {
//        MaterialAlertDialogBuilder(this)
//            .setTitle("Email Not Verified")
//            .setMessage("Please verify your email address before logging in. Check your inbox for the verification email.")
//            .setPositiveButton("OK", null)
//            .setNegativeButton("Resend Email") { _, _ ->
//                // TODO: Implement resend verification email
//                Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
//            }
//            .show()
//    }

    private fun setupRegisterText() {
        val fullText = "${getString(R.string.new_member)} ${getString(R.string.register_now)}"
        val spannableString = SpannableString(fullText)

        val linkColor = ContextCompat.getColor(this, R.color.link_color)

        // Find and make "Register now" clickable
        val registerStart = fullText.indexOf(getString(R.string.register_now))
        val registerEnd = registerStart + getString(R.string.register_now).length

        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
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
}