package com.hcmus.forumus_client.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import com.hcmus.forumus_client.databinding.LayoutBottomInputBarBinding

/**
 * A reusable bottom input bar component for text input + send action.
 *
 * Features:
 * - Editable text field with dynamic hint
 * - Send button callback returning the typed text
 * - Utility methods for focusing the input and showing the keyboard
 * - Clears input automatically after sending
 */
class BottomInputBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutBottomInputBarBinding =
        LayoutBottomInputBarBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * Callback triggered when the user taps the send button.
     * Provides the trimmed input text. Empty strings are filtered out.
     */
    /**
     * Callback triggered when the user taps the send button.
     * Provides the trimmed input text. Empty strings are filtered out.
     */
    var onSendClick: ((String) -> Unit)? = null

    /**
     * Callback triggered when the user cancels the reply mode.
     */
    var onCancelReply: (() -> Unit)? = null

    init {
        orientation = VERTICAL // Changed to vertical to stack banner and input

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

    /** Updates the input field hint text. */
    fun setHint(hint: String) {
        binding.etCommentInput.hint = hint
    }

    /**
     * Shows the "Replying to <user>" banner above the input.
     * @param username The name of the user being replied to.
     */
    fun showReplyBanner(username: String) {
        binding.tvReplyContext.text = context.getString(
            com.hcmus.forumus_client.R.string.replying_to_format, 
            username
        )
        binding.replyBannerContainer.visibility = VISIBLE
        binding.replyDivider.visibility = VISIBLE
        focusAndShowKeyboard()
    }

    /**
     * Hides the reply banner.
     */
    fun hideReplyBanner() {
        binding.replyBannerContainer.visibility = GONE
        binding.replyDivider.visibility = GONE
    }
    
    /**
     * Check if reply banner is visible
     */
    fun isReplying(): Boolean = binding.replyBannerContainer.visibility == VISIBLE

    /**
     * Requests focus on the input field and shows the soft keyboard.
     */
    fun focusAndShowKeyboard() {
        binding.etCommentInput.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etCommentInput, InputMethodManager.SHOW_IMPLICIT)
    }

    /** Clears the current input text. */
    fun clearInput() {
        binding.etCommentInput.text.clear()
    }

    /** Returns the current text from the input field. */
    fun getInputText(): String = binding.etCommentInput.text.toString()
}
