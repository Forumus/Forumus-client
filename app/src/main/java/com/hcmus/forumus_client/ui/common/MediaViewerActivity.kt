package com.hcmus.forumus_client.ui.common

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.hcmus.forumus_client.R
import android.util.Log

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var ivMedia: ImageView
    private lateinit var vvMedia: VideoView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var tvCounter: TextView

    private var mediaList: List<MediaViewerItem> = emptyList()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_viewer)

        ivMedia = findViewById(R.id.ivMedia)
        vvMedia = findViewById(R.id.vvMedia)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnClose = findViewById(R.id.btnClose)
        tvCounter = findViewById(R.id.tvCounter)

        mediaList = intent.getParcelableArrayListExtra("media_items") ?: emptyList()
        currentIndex = intent.getIntExtra("start_index", 0).coerceIn(0, maxOf(0, mediaList.size - 1))

        btnPrev.setOnClickListener {
            if (currentIndex > 0) showMedia(--currentIndex)
        }

        btnNext.setOnClickListener {
            if (currentIndex < mediaList.size - 1) showMedia(++currentIndex)
        }

        btnClose.setOnClickListener { finish() }

        if (mediaList.isEmpty()) {
            finish()
            return
        }

        showMedia(currentIndex)
    }

    private fun showMedia(index: Int) {
        if (index < 0 || index >= mediaList.size) return

        val item = mediaList[index]
        tvCounter.text = "${index + 1}/${mediaList.size}"

        // Update arrow enabled state
        btnPrev.isEnabled = index > 0
        btnNext.isEnabled = index < mediaList.size - 1

        try {
            if (item.type == MediaViewerItem.Type.IMAGE) {
                vvMedia.visibility = View.GONE
                vvMedia.stopPlayback()
                ivMedia.visibility = View.VISIBLE
                ivMedia.load(item.imageUrl) {
                    crossfade(true)
                    error(R.drawable.error_image)
                }
            } else {
                ivMedia.visibility = View.GONE
                vvMedia.visibility = View.VISIBLE
                try {
                    vvMedia.setVideoURI(Uri.parse(item.videoUrl))
                    val mc = MediaController(this)
                    mc.setAnchorView(vvMedia)
                    vvMedia.setMediaController(mc)
                    vvMedia.requestFocus()
                    vvMedia.start()
                } catch (e: Exception) {
                    Log.e("MediaViewer", "Error playing video", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaViewer", "Error showing media", e)
        }
    }
}
