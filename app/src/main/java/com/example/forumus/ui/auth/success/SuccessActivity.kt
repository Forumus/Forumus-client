package com.example.forumus.ui.auth.success

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.forumus.databinding.ActivitySuccessBinding
import com.example.forumus.ui.home.HomeActivity

class SuccessActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnFinish.setOnClickListener {
            // Navigate back to home screen after success
            val intent = android.content.Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
