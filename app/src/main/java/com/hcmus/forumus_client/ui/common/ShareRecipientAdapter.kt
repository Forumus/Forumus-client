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

    fun setFullList(recipients: List<User>) {
        fullRecipientList = recipients
        submitList(recipients.toList())
    }

    fun getFullList(): List<User> = fullRecipientList

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

    fun clearSelection() {
        selectedSet.clear()
        notifyDataSetChanged()
    }

    fun getSelectedUserIds(): List<String> = selectedSet.toList()

    inner class ViewHolder(private val binding: ItemShareRecipientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recipient: User) {
            binding.apply {
                tvName.text = recipient.fullName
                
                tvEmail.text = recipient.email

                avatar.load(recipient.profilePictureUrl) {
                    placeholder(R.drawable.default_avatar)
                    error(R.drawable.default_avatar)
                    crossfade(true)
                }
                
                val isSelected = selectedSet.contains(recipient.uid)
                ivCheckmark.visibility = if (isSelected) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                root.setOnClickListener {
                    val selected = !selectedSet.contains(recipient.uid)
                    if (selected) {
                        selectedSet.add(recipient.uid)
                    } else {
                        selectedSet.remove(recipient.uid)
                    }
                    
                    ivCheckmark.visibility = if (selected) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    
                    onSelectionChanged(recipient.uid, selected)
                }
            }
        }
    }

    class RecipientDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
