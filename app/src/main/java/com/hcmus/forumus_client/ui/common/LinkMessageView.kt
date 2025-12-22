package com.hcmus.forumus_client.ui.common

import android.content.Context
import android.graphics.Paint
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.hcmus.forumus_client.utils.SharePostUtil

/**
 * Custom view for displaying messages that may contain share links.
 * Detects share URLs and renders them as blue, underlined, clickable text.
 */
class LinkMessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    
    private var onLinkClickListener: ((String) -> Unit)? = null
    
    init {
        // Enable link movement to handle click events
        movementMethod = LinkMovementMethod.getInstance()
    }
    
    /**
     * Sets the message text and processes it for link rendering
     * @param message The message text to display
     */
    fun setMessageText(message: String) {
        if (SharePostUtil.isShareUrl(message)) {
            // This is a share URL - render as link
            renderAsLink(message)
        } else {
            // Regular text
            text = message
        }
    }
    
    /**
     * Sets a listener for when a link is clicked
     * @param listener Callback with the clicked URL
     */
    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
    }
    
    /**
     * Renders the message as a clickable, blue, underlined link
     * @param url The URL to display and handle clicks for
     */
    private fun renderAsLink(url: String) {
        val spannableString = SpannableString(url)
        
        // Add underline
        spannableString.setSpan(
            UnderlineSpan(),
            0,
            url.length,
            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
        )
        
        // Add custom click span
        val clickableSpan = object : android.text.style.ClickableSpan() {
            override fun onClick(widget: android.view.View) {
                onLinkClickListener?.invoke(url)
            }
        }
        
        spannableString.setSpan(
            clickableSpan,
            0,
            url.length,
            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
        )
        
        // Set text and properties
        text = spannableString
        
        // Make text blue
        setTextColor(android.graphics.Color.BLUE)
    }
}
