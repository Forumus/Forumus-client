package com.hcmus.forumus_client.ui.common

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.data.repository.ChatRepository
import com.hcmus.forumus_client.data.repository.UserRepository
import com.hcmus.forumus_client.utils.SharePostUtil
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

/**
 * Dialog for selecting recipients to share a post with.
 * Shows a list of mock recipients (in a real app, this would be contacts/chats).
 * Features a custom UI with search, selection counter, and recipient filtering.
 */
class SharePostDialog : DialogFragment() {
    
    private var postId: String = ""
    private lateinit var recipientAdapter: ShareRecipientAdapter
    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    
    // UI Components
    private lateinit var etSearchRecipient: EditText
    private lateinit var rvShareRecipients: RecyclerView
    private lateinit var tvSelectionCount: TextView
    private lateinit var ivClearSelection: ImageView
    private lateinit var llSelectionCounter: LinearLayout
    private lateinit var btnShare: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: View
    
    // Track sharing state
    private var isSharing = false

    companion object {
        private const val ARG_POST_ID = "postId"

        fun newInstance(postId: String): SharePostDialog {
            return SharePostDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_ID, postId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString(ARG_POST_ID) ?: ""
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        
        // Inflate custom layout
        val customView = layoutInflater.inflate(R.layout.dialog_share_post, null)
        
        // Bind UI components
        etSearchRecipient = customView.findViewById(R.id.etSearchRecipient)
        rvShareRecipients = customView.findViewById(R.id.rvShareRecipients)
        tvSelectionCount = customView.findViewById(R.id.tvSelectionCount)
        ivClearSelection = customView.findViewById(R.id.ivClearSelection)
        llSelectionCounter = customView.findViewById(R.id.llSelectionCounter)
        btnShare = customView.findViewById(R.id.btnShare)
        btnCancel = customView.findViewById(R.id.btnCancel)
        progressBar = customView.findViewById(R.id.progressBar)
        
        // Setup RecyclerView
        rvShareRecipients.layoutManager = LinearLayoutManager(context)
        
        // Create adapter with callback
        // Adapter manages selection state, just update UI counter on changes
        recipientAdapter = ShareRecipientAdapter { _, _ ->
            updateSelectionCounter()
        }
        
        rvShareRecipients.adapter = recipientAdapter
        
        // Fetch recipients from database
        lifecycleScope.launch {
            val recipients = SharePostUtil.getRecipients(userRepository)
            recipientAdapter.setFullList(recipients)
        }
        
        // Setup search filter
        etSearchRecipient.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recipientAdapter.filterRecipients(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Setup clear selection button
        ivClearSelection.setOnClickListener {
            recipientAdapter.clearSelection()
            updateSelectionCounter()
        }
        
        // Setup button clicks
        btnCancel.setOnClickListener {
            dismiss()
        }
        
        btnShare.setOnClickListener {
            val selectedIds = recipientAdapter.getSelectedUserIds()
            if (selectedIds.isNotEmpty()) {
                sharePostToRecipients(selectedIds)
            } else {
                Toast.makeText(context, "Please select at least one recipient", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize counter and button state
        updateSelectionCounter()
        
        // Create dialog
        val dialog = AlertDialog.Builder(context)
            .setView(customView)
            .create()
        
        // Optional: Set dialog size and properties
        dialog.window?.attributes?.apply {
            width = (context.resources.displayMetrics.widthPixels * 0.95).toInt()
            height = (context.resources.displayMetrics.heightPixels * 0.7).toInt()
        }
        
        return dialog
    }
    
    /**
     * Updates the selection counter display
     */
    private fun updateSelectionCounter() {
        val count = recipientAdapter.getSelectedUserIds().size
        tvSelectionCount.text = getString(R.string.contacts_selected_format, count)
        
        // Show/hide clear button based on selection
        ivClearSelection.visibility = if (count > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Update share button state - change background based on count
        if (count > 0) {
            btnShare.isEnabled = true
            btnShare.setBackgroundResource(R.drawable.button_login_background)
            btnShare.setTextColor(Color.WHITE)
        } else {
            btnShare.isEnabled = false
            btnShare.setBackgroundResource(R.drawable.button_share_post_background_gray)
            btnShare.setTextColor(Color.parseColor("#666666"))
        }
    }

    private fun sharePostToRecipients(selectedIds: List<String>) {
        // Prevent duplicate submissions
        if (isSharing) return
        
        lifecycleScope.launch {
            // Show loading state
            isSharing = true
            setLoadingState(true)
            
            var successCount = 0
            var failureCount = 0
            
            for (recipientId in selectedIds) {
                val result = SharePostUtil.sendShareMessage(
                    recipientId = recipientId,
                    postId = postId,
                    chatRepository = chatRepository
                )
                
                if (result.isSuccess) {
                    successCount++
                } else {
                    failureCount++
                    Log.d("SharePostDialog", "Failed to share with recipient: ${result.exceptionOrNull()?.message}")
                }
            }
            
            // Hide loading state
            isSharing = false
            setLoadingState(false)
            
            // Show result toast
            val message = when {
                failureCount == 0 -> "Post shared successfully to $successCount contact${if (successCount > 1) "s" else ""}"
                successCount == 0 -> "Failed to share post to $failureCount contact${if (failureCount > 1) "s" else ""}"
                else -> "Shared with $successCount contact${if (successCount > 1) "s" else ""}, $failureCount failed"
            }
            
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
    
    /**
     * Sets the loading state of the dialog.
     * Disables buttons and shows progress indicator during sharing.
     */
    private fun setLoadingState(loading: Boolean) {
        btnShare.isEnabled = !loading
        btnCancel.isEnabled = !loading
        etSearchRecipient.isEnabled = !loading
        rvShareRecipients.isEnabled = !loading
        ivClearSelection.isEnabled = !loading
        
        if (loading) {
            progressBar.visibility = View.VISIBLE
            btnShare.text = "Sharing..."
            btnShare.alpha = 0.6f
        } else {
            progressBar.visibility = View.GONE
            btnShare.text = "Share"
            btnShare.alpha = 1.0f
        }
    }
}