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

class LinkMessageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {
    
    private var onLinkClickListener: ((String) -> Unit)? = null
    
    init {
        movementMethod = LinkMovementMethod.getInstance()
    }
    
    fun setMessageText(message: String) {
        if (SharePostUtil.isShareUrl(message)) {
            renderAsLink(message)
        } else {
            text = message
        }
    }
    
    fun setOnLinkClickListener(listener: (String) -> Unit) {
        onLinkClickListener = listener
    }
    
    private fun renderAsLink(url: String) {
        val spannableString = SpannableString(url)
        
        spannableString.setSpan(
            UnderlineSpan(),
            0,
            url.length,
            SpannableString.SPAN_INCLUSIVE_INCLUSIVE
        )
        
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
        
        text = spannableString
        setTextColor(android.graphics.Color.BLUE)
    }
}
