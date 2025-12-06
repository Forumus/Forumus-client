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
    var onSendClick: ((String) -> Unit)? = null

    init {
        orientation = HORIZONTAL

        binding.btnSendComment.setOnClickListener {
            val text = getInputText().trim()
            if (text.isNotEmpty()) {
                onSendClick?.invoke(text)
                clearInput()
            }
        }
    }

    /** Updates the input field hint text. */
    fun setHint(hint: String) {
        binding.etCommentInput.hint = hint
    }

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
