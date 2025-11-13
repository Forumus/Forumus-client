package com.example.forumus.ui.auth.register

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
import com.example.forumus.R
import com.example.forumus.data.model.UserRole
import com.example.forumus.databinding.ActivityRegisterBinding
import com.example.forumus.ui.auth.login.LoginActivity
import com.example.forumus.ui.auth.verification.VerificationActivity
import com.example.forumus.utils.Resource

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupLoginText()
        observeRegisterState()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val isTermsAccepted = binding.cbTerms.isChecked

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
                    startActivity(intent)
                    finish() // Close registration activity
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
        binding.btnRegister.isEnabled = !isLoading
    }

    private fun setupLoginText() {
        val fullText = "${getString(R.string.already_member)} ${getString(R.string.login_now)}"
        val spannableString = SpannableString(fullText)

        val linkColor = ContextCompat.getColor(this, R.color.link_color)

        val loginStart = fullText.indexOf(getString(R.string.already_member))
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

}