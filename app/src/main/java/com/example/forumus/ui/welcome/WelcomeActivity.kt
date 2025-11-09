package com.example.forumus.ui.welcome

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.forumus.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPrivacyPolicyText()
        binding.btnGetStarted.setOnClickListener {
            // TODO: Navigate to next screen (e.g. Register/Login)
        }
    }

    private fun setupPrivacyPolicyText() {
        val text = getString(R.string.privacy_policy_text)
        val start = text.indexOf("Privacy Policy")
        val end = start + "Privacy Policy".length
        val spannable = SpannableString(text)
        spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(this@WelcomeActivity, "Privacy Policy clicked", Toast.LENGTH_SHORT).show()
                // TODO: Open privacy policy
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvPrivacyPolicy.text = spannable
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
    }
}
