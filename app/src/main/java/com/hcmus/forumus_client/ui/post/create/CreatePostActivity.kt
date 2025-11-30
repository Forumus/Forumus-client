package com.hcmus.forumus_client.ui.post.create

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.hcmus.forumus_client.R

class CreatePostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post) // Đảm bảo đúng tên file XML của bạn

        // 1. Tìm nút Đóng/Quay về (Dấu X hoặc mũi tên Back)
        // Trong file XML bạn gửi, ID của nó là "btnClose"
        val btnClose = findViewById<ImageView>(R.id.btnClose)

        // 2. Bắt sự kiện click
        btnClose.setOnClickListener {
            // Lệnh này sẽ đóng màn hình hiện tại lại
            // Màn hình Home (đang nằm bên dưới) sẽ tự động hiện ra
            finish()
        }
    }
}