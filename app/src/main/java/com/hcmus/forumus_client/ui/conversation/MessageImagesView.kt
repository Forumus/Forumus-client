package com.hcmus.forumus_client.ui.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.hcmus.forumus_client.R

class MessageImagesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var imageUrls: List<String> = emptyList()
    private var onImageClickListener: ((List<String>, Int) -> Unit)? = null

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun setImageUrls(urls: List<String>) {
        if (urls == imageUrls) return
        imageUrls = urls
        removeAllViews()
        
        when {
            urls.isEmpty() -> {
                visibility = View.GONE
            }
            urls.size == 1 -> {
                visibility = View.VISIBLE
                setupSingleImage(urls[0])
            }
            urls.size == 2 -> {
                visibility = View.VISIBLE
                setupTwoImages(urls)
            }
            urls.size == 3 -> {
                visibility = View.VISIBLE
                setupThreeImages(urls)
            }
            else -> {
                visibility = View.VISIBLE
                setupFourOrMoreImages(urls)
            }
        }
    }

    fun setOnImageClickListener(listener: (List<String>, Int) -> Unit) {
        onImageClickListener = listener
    }

    private fun setupSingleImage(url: String) {
        val imageView = createImageView(0, 0) // Dimensions will be set by LayoutParams
        loadImage(imageView, url, 0)
        
        val layoutParams = LayoutParams(LayoutParams.MATCH_CONSTRAINT, 200.dpToPx())
        layoutParams.startToStart = LayoutParams.PARENT_ID
        layoutParams.endToEnd = LayoutParams.PARENT_ID
        layoutParams.topToTop = LayoutParams.PARENT_ID
        layoutParams.dimensionRatio = "3:2" // Maintain aspect ratio
        
        addView(imageView, layoutParams)
    }

    private fun setupTwoImages(urls: List<String>) {
        val imageSize = 145.dpToPx()
        val spacing = 4.dpToPx()
        
        // First image
        val imageView1 = createImageView(imageSize, imageSize)
        loadImage(imageView1, urls[0], 0)
        imageView1.id = View.generateViewId()
        
        val params1 = LayoutParams(imageSize, imageSize)
        params1.startToStart = LayoutParams.PARENT_ID
        params1.topToTop = LayoutParams.PARENT_ID
        params1.marginEnd = spacing / 2
        
        addView(imageView1, params1)
        
        // Second image
        val imageView2 = createImageView(imageSize, imageSize)
        loadImage(imageView2, urls[1], 1)
        
        val params2 = LayoutParams(imageSize, imageSize)
        params2.startToEnd = imageView1.id
        params2.topToTop = LayoutParams.PARENT_ID
        params2.marginStart = spacing / 2
        
        addView(imageView2, params2)
    }

    private fun setupThreeImages(urls: List<String>) {
        val imageSize = 145.dpToPx()
        val spacing = 4.dpToPx()
        
        // First image (larger, on left)
        val imageView1 = createImageView(imageSize, imageSize * 2 + spacing)
        loadImage(imageView1, urls[0], 0)
        imageView1.id = View.generateViewId()
        
        val params1 = LayoutParams(imageSize, imageSize * 2 + spacing)
        params1.startToStart = LayoutParams.PARENT_ID
        params1.topToTop = LayoutParams.PARENT_ID
        params1.marginEnd = spacing / 2
        
        addView(imageView1, params1)
        
        // Second image (top right)
        val imageView2 = createImageView(imageSize, imageSize)
        loadImage(imageView2, urls[1], 1)
        imageView2.id = View.generateViewId()
        
        val params2 = LayoutParams(imageSize, imageSize)
        params2.startToEnd = imageView1.id
        params2.topToTop = LayoutParams.PARENT_ID
        params2.marginStart = spacing / 2
        params2.bottomToTop = View.generateViewId()
        
        addView(imageView2, params2)
        
        // Third image (bottom right)
        val imageView3 = createImageView(imageSize, imageSize)
        loadImage(imageView3, urls[2], 2)
        
        val params3 = LayoutParams(imageSize, imageSize)
        params3.startToEnd = imageView1.id
        params3.topToBottom = imageView2.id
        params3.marginStart = spacing / 2
        params3.topMargin = spacing
        
        addView(imageView3, params3)
    }

    private fun setupFourOrMoreImages(urls: List<String>) {
        val imageSize = 145.dpToPx()
        val spacing = 4.dpToPx()
        
        // Create 2x2 grid
        val positions = listOf(
            Pair(0, 0), // Top-left
            Pair(1, 0), // Top-right
            Pair(0, 1), // Bottom-left
            Pair(1, 1)  // Bottom-right
        )
        
        val imageViews = mutableListOf<View>()
        
        for (i in 0 until minOf(4, urls.size)) {
            val (col, row) = positions[i]
            
            // For the 4th image with more images, wrap in FrameLayout with overlay
            if (i == 3 && urls.size > 4) {
                val frameLayout = FrameLayout(context)
                frameLayout.id = View.generateViewId()
                
                val imageView = createImageView(imageSize, imageSize)
                loadImage(imageView, urls[i], i)
                
                // Create overlay
                val overlay = View(context)
                overlay.setBackgroundColor(0x99000000.toInt())
                overlay.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                
                // Create +N text
                val remainingCount = urls.size - 4
                val textView = TextView(context)
                textView.text = "+$remainingCount"
                textView.setTextColor(0xFFFFFFFF.toInt())
                textView.textSize = 24f
                textView.gravity = android.view.Gravity.CENTER
                val textParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                textView.layoutParams = textParams
                
                frameLayout.addView(imageView)
                frameLayout.addView(overlay)
                frameLayout.addView(textView)
                
                frameLayout.setOnClickListener {
                    onImageClickListener?.invoke(imageUrls, i)
                }
                
                val params = LayoutParams(imageSize, imageSize)
                
                if (col == 0) {
                    params.startToStart = LayoutParams.PARENT_ID
                    params.marginEnd = spacing / 2
                } else {
                    params.startToEnd = imageViews[i - 1].id
                    params.marginStart = spacing / 2
                }
                
                if (row == 0) {
                    params.topToTop = LayoutParams.PARENT_ID
                } else {
                    params.topToBottom = imageViews[if (col == 0) 0 else 1].id
                    params.topMargin = spacing
                }
                
                addView(frameLayout, params)
                imageViews.add(frameLayout)
            } else {
                val imageView = createImageView(imageSize, imageSize)
                loadImage(imageView, urls[i], i)
                imageView.id = View.generateViewId()
                
                val params = LayoutParams(imageSize, imageSize)
                
                if (col == 0) {
                    params.startToStart = LayoutParams.PARENT_ID
                    params.marginEnd = spacing / 2
                } else {
                    params.startToEnd = imageViews[i - 1].id
                    params.marginStart = spacing / 2
                }
                
                if (row == 0) {
                    params.topToTop = LayoutParams.PARENT_ID
                } else {
                    params.topToBottom = imageViews[if (col == 0) 0 else 1].id
                    params.topMargin = spacing
                }
                
                addView(imageView, params)
                imageViews.add(imageView)
            }
        }
    }

    private fun createImageView(width: Int, height: Int): ImageView {
        val imageView = ImageView(context)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setBackgroundResource(R.drawable.message_image_background)
        imageView.clipToOutline = true
        imageView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        return imageView
    }

    private fun loadImage(imageView: ImageView, url: String, position: Int) {
        val cornerRadius = 16.dpToPx()
        
        val requestOptions = RequestOptions()
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .timeout(10000)
            .transform(RoundedCorners(cornerRadius))
        
        Glide.with(context)
            .load(url)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
        
        imageView.setOnClickListener {
            onImageClickListener?.invoke(imageUrls, position)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
