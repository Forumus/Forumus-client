package com.hcmus.forumus_client.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.ItemUserSearchBinding // Cần tạo layout item_user_search.xml
import com.hcmus.forumus_client.data.model.User // Đảm bảo bạn có model User

class UserAdapter(
    private var users: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    fun submitList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(private val binding: ItemUserSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textUserName.text = user.fullName.ifEmpty { "Unknown User" }
            binding.textUserEmail.text = user.email

            // Load Avatar
            binding.imageProfile.load(user.profilePictureUrl) {
                placeholder(R.drawable.ic_default_profile)
                error(R.drawable.ic_default_profile)
                transformations(coil.transform.CircleCropTransformation())
            }

            binding.root.setOnClickListener { onClick(user) }
        }
    }
}