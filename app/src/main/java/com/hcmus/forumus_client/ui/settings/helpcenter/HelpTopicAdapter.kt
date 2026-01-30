package com.hcmus.forumus_client.ui.settings.helpcenter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.databinding.ItemHelpTopicBinding

/**
 * Adapter for expandable help center FAQs with collapse/expand animation.
 */
class HelpTopicAdapter(
    private val topics: List<HelpTopic>
) : RecyclerView.Adapter<HelpTopicAdapter.HelpTopicViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpTopicViewHolder {
        val binding = ItemHelpTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HelpTopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HelpTopicViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount(): Int = topics.size

    inner class HelpTopicViewHolder(
        private val binding: ItemHelpTopicBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(topic: HelpTopic) {
            // Set icon and title
            binding.tvTitle.text = topic.title
            binding.tvContent.text = topic.content

            // Set initial expand/collapse state
            updateExpandState(topic.isExpanded, animate = false)

            // Handle click to expand/collapse
            binding.root.setOnClickListener {
                topic.isExpanded = !topic.isExpanded
                updateExpandState(topic.isExpanded, animate = true)
            }
        }

        private fun updateExpandState(isExpanded: Boolean, animate: Boolean) {
            if (isExpanded) {
                // Show content
                binding.tvContent.visibility = View.VISIBLE
                
                // Rotate chevron down
                if (animate) {
                    binding.ivChevron.animate()
                        .rotation(180f)
                        .setDuration(250)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                } else {
                    binding.ivChevron.rotation = 180f
                }
            } else {
                // Hide content
                binding.tvContent.visibility = View.GONE
                
                // Rotate chevron up
                if (animate) {
                    binding.ivChevron.animate()
                        .rotation(0f)
                        .setDuration(250)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                } else {
                    binding.ivChevron.rotation = 0f
                }
            }
        }
    }
}
