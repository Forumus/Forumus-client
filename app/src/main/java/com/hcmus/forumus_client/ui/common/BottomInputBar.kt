package com.hcmus.forumus_client.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.hcmus.forumus_client.databinding.LayoutBottomInputBarBinding

class BottomInputBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutBottomInputBarBinding =
        LayoutBottomInputBarBinding.inflate(LayoutInflater.from(context), this, true)

    var onSendClick: ((String) -> Unit)? = null
    var onCancelReply: (() -> Unit)? = null

    init {
        orientation = VERTICAL

        binding.btnSendComment.setOnClickListener {
            val text = getInputText().trim()
            if (text.isNotEmpty()) {
                onSendClick?.invoke(text)
                clearInput()
            }
        }

        binding.btnCancelReply.setOnClickListener {
            hideReplyBanner()
            onCancelReply?.invoke()
        }
    }

    fun setHint(hint: String) {
        binding.etCommentInput.hint = hint
    }

    fun showReplyBanner(username: String) {
        binding.tvReplyContext.text = context.getString(
            com.hcmus.forumus_client.R.string.replying_to_format, 
            username
        )
        binding.replyBannerContainer.visibility = VISIBLE
        binding.replyDivider.visibility = VISIBLE
        focusAndShowKeyboard()
    }

    fun hideReplyBanner() {
        binding.replyBannerContainer.visibility = GONE
        binding.replyDivider.visibility = GONE
    }
    
    fun isReplying(): Boolean = binding.replyBannerContainer.visibility == VISIBLE

    fun focusAndShowKeyboard() {
        binding.etCommentInput.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etCommentInput, InputMethodManager.SHOW_IMPLICIT)
    }

    fun clearInput() {
        binding.etCommentInput.text.clear()
    }

    fun getInputText(): String = binding.etCommentInput.text.toString()
}
