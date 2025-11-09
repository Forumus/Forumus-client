package com.example.forumus.ui.auth.register

import android.os.Bundle
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.forumus.R

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupLoginText() {
        val loginTv = findViewById<TextView>(R.id.tv_log_in)
        val spanString = SpannableString(getString(R.string.already_member_login))

        val loginClick = object : ClickableSpan() {
            override fun onClick(widget: android.view.View) {
                Toast.makeText(this@RegisterActivity, "Login clicked", Toast.LENGTH_SHORT).show()
            }
        }

        val startIndex = spanString.indexOf("Log In")
        val endIndex = startIndex + "Log In".length
        spanString.setSpan(loginClick, startIndex, endIndex, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}