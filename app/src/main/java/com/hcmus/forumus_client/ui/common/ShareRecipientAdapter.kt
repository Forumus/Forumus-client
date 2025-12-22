package com.hcmus.forumus_client.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.data.model.User
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ItemShareRecipientBinding
import coil.load

/**
 * Adapter for displaying a list of recipients to share a post with.
 * Each recipient has a checkbox to select/deselect.
 * Uses ListAdapter with DiffUtil for efficient updates and filtering.
 */
class ShareRecipientAdapter(
    private val onSelectionChanged: (userId: String, isSelected: Boolean) -> Unit
) : ListAdapter<User, ShareRecipientAdapter.ViewHolder>(RecipientDiffCallback()) {

    private val selectedSet = mutableSetOf<String>()
    private var fullRecipientList = listOf<User>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShareRecipientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipient = getItem(position)
        holder.bind(recipient)
    }

    /**
     * Set the full list of recipients (unfiltered).
     * Used for initial population and clearing filters.
     */
    fun setFullList(recipients: List<User>) {
        fullRecipientList = recipients
        submitList(recipients.toList())
    }

    /**
     * Get the full unfiltered list of recipients.
     */
    fun getFullList(): List<User> = fullRecipientList

    /**
     * Filter recipients by name or email.
     * If query is empty, shows all recipients.
     */
    fun filterRecipients(query: String) {
        if (query.isEmpty()) {
            submitList(fullRecipientList.toList())
        } else {
            val filtered = fullRecipientList.filter { user ->
                user.fullName.contains(query, ignoreCase = true) ||
                user.email.contains(query, ignoreCase = true)
            }
            submitList(filtered)
        }
    }

    /**
     * Clear all selections (uncheck all checkboxes).
     */
    fun clearSelection() {
        selectedSet.clear()
        notifyDataSetChanged()
    }

    /**
     * Get all selected user IDs.
     */
    fun getSelectedUserIds(): List<String> = selectedSet.toList()

    inner class ViewHolder(private val binding: ItemShareRecipientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipient: User) {
            binding.apply {
                // Set recipient name
                tvName.text = recipient.fullName
                
                // Set recipient email
                tvEmail.text = recipient.email

                // Load recipient avatar
                avatar.load(recipient.profilePictureUrl) {
                    placeholder(R.drawable.default_avatar)
                    error(R.drawable.default_avatar)
                    crossfade(true)
                }
                
                // Update checkmark icon visibility based on selectedSet
                val isSelected = selectedSet.contains(recipient.uid)
                ivCheckmark.visibility = if (isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                // Handle row click to toggle selection
                root.setOnClickListener {
                    val selected = !selectedSet.contains(recipient.uid)
                    if (selected) {
                        selectedSet.add(recipient.uid)
                    } else {
                        selectedSet.remove(recipient.uid)
                    }
                    
                    // Update icon visibility
                    ivCheckmark.visibility = if (selected) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    
                    // Notify change
                    onSelectionChanged(recipient.uid, selected)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     * Compares User objects by their unique ID.
     */
    class RecipientDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
