package com.hcmus.forumus_client.ui.post.create

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.hcmus.forumus_client.data.model.TopicItem
import com.hcmus.forumus_client.databinding.ItemTopicGridBinding

class TopicAdapter(
    private val topics: List<TopicItem>,
    private val maxSelection: Int = 5
) : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemTopicGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount(): Int = topics.size

    fun getSelectedTopics(): List<String> {
        return topics.filter { it.isSelected }.map { it.name }
    }

    inner class TopicViewHolder(private val binding: ItemTopicGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TopicItem) {
            binding.tvTopicName.text = item.name
            binding.tvTopicIcon.text = item.icon

            // Xử lý UI khi được chọn hoặc không
            updateSelectionState(binding.cardTopic, item.isSelected)

            binding.root.setOnClickListener {
                if (!item.isSelected) {
                    // Đang chưa chọn -> Muốn chọn
                    val currentCount = topics.count { it.isSelected }
                    if (currentCount >= maxSelection) {
                        Toast.makeText(binding.root.context, "Max $maxSelection topics!", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    item.isSelected = true
                } else {
                    // Đang chọn -> Bỏ chọn
                    item.isSelected = false
                }
                // Cập nhật lại UI ngay lập tức
                updateSelectionState(binding.cardTopic, item.isSelected)
            }
        }

        private fun updateSelectionState(card: MaterialCardView, isSelected: Boolean) {
            if (isSelected) {
                // Màu xanh giống ảnh 2 (#2196F3)
                card.strokeColor = Color.parseColor("#2196F3")
                card.strokeWidth = 4 // Viền dày lên
                card.setCardBackgroundColor(Color.parseColor("#E3F2FD")) // Nền xanh nhạt
            } else {
                // Trạng thái bình thường
                card.strokeColor = Color.parseColor("#E0E0E0")
                card.strokeWidth = 2
                card.setCardBackgroundColor(Color.WHITE)
            }
        }
    }
}