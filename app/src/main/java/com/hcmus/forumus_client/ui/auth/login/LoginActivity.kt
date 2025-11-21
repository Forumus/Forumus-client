package com.hcmus.forumus_client.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupRegisterText()
        setupForgotPasswordText()
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
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
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