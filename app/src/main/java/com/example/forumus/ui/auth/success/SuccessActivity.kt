package com.example.forumus.ui.auth.success

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.forumus.databinding.ActivitySuccessBinding

class SuccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFinish.setOnClickListener {
            finish()
        }
    }
}
