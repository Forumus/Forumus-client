package com.example.forumus.ui.slide

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.forumus.databinding.ActivitySlideBinding

class SlideActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySlideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNext.setOnClickListener {
            // TODO: Navigate to next onboarding screen or main app
        }
    }
}
