package com.hcmus.forumus_client.ui.onboarding.slide

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ActivitySlideBinding
import com.hcmus.forumus_client.ui.auth.login.LoginActivity
import com.hcmus.forumus_client.data.local.PreferencesManager

class SlideActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlideBinding
    private lateinit var preferenceManager: PreferencesManager
    private var currentSlide = 1
    private val totalSlides = 3

    // Slide data
    private val slideTitles = arrayOf(
        R.string.slide_title_1,
        R.string.slide_title_2,
        R.string.slide_title_3
    )

    private val slideSubtitles = arrayOf(
        R.string.slide_subtitle_1,
        R.string.slide_subtitle_2,
        R.string.slide_subtitle_3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_slide)

        preferenceManager = PreferencesManager(this)
        
        setupUI()
        initializeFirstSlide()
    }
    
    private fun initializeFirstSlide() {
        // Set initial content without animation
        binding.tvTitle.text = getString(slideTitles[currentSlide - 1])
        binding.tvSubtitle.text = getString(slideSubtitles[currentSlide - 1])
        updateIndicators()
    }

    private fun setupUI() {
        // Set up next button click
        binding.btnNext.setOnClickListener {
            if (currentSlide < totalSlides) {
                currentSlide++
                updateSlideContent()
            } else {
                // Navigate to login activity after last slide
                navigateToLogin()
            }
        }
    }

    private fun updateSlideContent() {
        // Animate text change with fade effect
        animateTextChange()
        
        // Update indicators
        updateIndicators()
    }
    
    private fun animateTextChange() {
        val fadeOutDuration = 200L
        val fadeInDuration = 300L
        
        // Create fade out animations
        val titleFadeOut = ObjectAnimator.ofFloat(binding.tvTitle, View.ALPHA, 1f, 0f)
        val subtitleFadeOut = ObjectAnimator.ofFloat(binding.tvSubtitle, View.ALPHA, 1f, 0f)
        
        titleFadeOut.duration = fadeOutDuration
        subtitleFadeOut.duration = fadeOutDuration
        
        // Create fade in animations
        val titleFadeIn = ObjectAnimator.ofFloat(binding.tvTitle, View.ALPHA, 0f, 1f)
        val subtitleFadeIn = ObjectAnimator.ofFloat(binding.tvSubtitle, View.ALPHA, 0f, 1f)
        
        titleFadeIn.duration = fadeInDuration
        subtitleFadeIn.duration = fadeInDuration
        
        // Create fade out animation set
        val fadeOutSet = AnimatorSet()
        fadeOutSet.playTogether(titleFadeOut, subtitleFadeOut)
        
        // Create fade in animation set
        val fadeInSet = AnimatorSet()
        fadeInSet.playTogether(titleFadeIn, subtitleFadeIn)
        
        // Start fade out, then update text, then fade in
        fadeOutSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
            
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Update text content during fade out
                binding.tvTitle.text = getString(slideTitles[currentSlide - 1])
                binding.tvSubtitle.text = getString(slideSubtitles[currentSlide - 1])
                
                // Start fade in animation
                fadeInSet.start()
            }
        })
        
        // Start the fade out animation
        fadeOutSet.start()
    }

    private fun updateIndicators() {
        val activeDrawable = ContextCompat.getDrawable(this, R.drawable.indicator_active)
        val inactiveDrawable = ContextCompat.getDrawable(this, R.drawable.indicator_inactive)

        // Reset all indicators to inactive
        binding.indicator1.background = inactiveDrawable
        binding.indicator2.background = inactiveDrawable
        binding.indicator3.background = inactiveDrawable

        // Set current indicator to active
        when (currentSlide) {
            1 -> binding.indicator1.background = activeDrawable
            2 -> binding.indicator2.background = activeDrawable
            3 -> binding.indicator3.background = activeDrawable
        }
    }

    private fun navigateToLogin() {
        // Mark that user has completed onboarding
        preferenceManager.isFirstTime = false
        
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
