package com.hcmus.forumus_client.ui.onboarding.welcome

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivityWelcomeBinding
import com.hcmus.forumus_client.ui.onboarding.slide.SlideActivity

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_welcome)
        
        setupUI()
    }

    private fun setupUI() {
        // Set up privacy policy text with HTML formatting
        binding.tvPrivacyPolicy.text = Html.fromHtml(
            getString(com.hcmus.forumus_client.R.string.privacy_policy_text),
            Html.FROM_HTML_MODE_LEGACY
        )
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()

        // Set up get started button click
        binding.btnGetStarted.setOnClickListener {
            navigateToSlideActivity()
        }
    }

    private fun navigateToSlideActivity() {
        val intent = Intent(this, SlideActivity::class.java)
        startActivity(intent)
        finish()
    }
}
