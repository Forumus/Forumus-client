package com.hcmus.forumus_client.ui.image

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.hcmus.forumus_client.databinding.ActivityFullscreenImageBinding

class FullscreenImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenImageBinding
    private lateinit var imageAdapter: FullscreenImageAdapter
    private var imageUrls: List<String> = emptyList()
    private var initialPosition: Int = 0
    private var overlaysVisible = true

    companion object {
        private const val EXTRA_IMAGE_URLS = "extra_image_urls"
        private const val EXTRA_INITIAL_POSITION = "extra_initial_position"

        fun createIntent(context: Context, imageUrls: List<String>, initialPosition: Int = 0): Intent {
            return Intent(context, FullscreenImageActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_IMAGE_URLS, ArrayList(imageUrls))
                putExtra(EXTRA_INITIAL_POSITION, initialPosition)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityFullscreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: emptyList()
        initialPosition = intent.getIntExtra(EXTRA_INITIAL_POSITION, 0)

        if (imageUrls.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupViewPager()
        setupClickListeners()
        hideSystemUI()
    }

    private fun setupUI() {
        // Handle system bars insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topOverlay.setPadding(
                systemBars.left,
                systemBars.top,
                binding.topOverlay.paddingRight,
                binding.topOverlay.paddingBottom
            )
            binding.bottomOverlay.setPadding(
                systemBars.left,
                binding.bottomOverlay.paddingTop,
                binding.bottomOverlay.paddingRight,
                systemBars.bottom
            )
            insets
        }

        updateImageCounter(initialPosition)
    }

    private fun setupViewPager() {
        imageAdapter = FullscreenImageAdapter(imageUrls) { 
            toggleOverlays()
        }
        
        binding.viewPagerImages.apply {
            adapter = imageAdapter
            setCurrentItem(initialPosition, false)
            
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateImageCounter(position)
                }
            })
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            shareCurrentImage()
        }
    }

    private fun updateImageCounter(position: Int) {
        binding.tvImageCounter.text = "${position + 1} / ${imageUrls.size}"
    }

    private fun toggleOverlays() {
        overlaysVisible = !overlaysVisible
        val visibility = if (overlaysVisible) View.VISIBLE else View.GONE
        
        binding.topOverlay.visibility = visibility
        
        if (overlaysVisible) {
            showSystemUI()
        } else {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun shareCurrentImage() {
        val currentPosition = binding.viewPagerImages.currentItem
        val currentImageUrl = imageUrls[currentPosition]
        
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, currentImageUrl)
                putExtra(Intent.EXTRA_SUBJECT, "Shared image")
            }
            startActivity(Intent.createChooser(shareIntent, "Share image"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}