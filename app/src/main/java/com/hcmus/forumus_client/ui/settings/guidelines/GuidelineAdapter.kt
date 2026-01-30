package com.hcmus.forumus_client.ui.settings.guidelines

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_client.databinding.ItemGuidelineSectionBinding

/**
 * Adapter for expandable community guideline sections with collapse/expand animation.
 */
class GuidelineAdapter(
    private val sections: List<GuidelineSection>
) : RecyclerView.Adapter<GuidelineAdapter.GuidelineViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuidelineViewHolder {
        val binding = ItemGuidelineSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GuidelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuidelineViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount(): Int = sections.size

    inner class GuidelineViewHolder(
        private val binding: ItemGuidelineSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: GuidelineSection) {
            // Set icon and title
            binding.ivIcon.setImageResource(section.icon)
            binding.tvTitle.text = section.title

            // Set up guidelines text
            val guidelinesText = section.guidelines.joinToString("\n") { "• $it" }
            binding.tvGuidelines.text = guidelinesText

            // Set initial expand/collapse state
            updateExpandState(section.isExpanded, animate = false)

            // Handle click to expand/collapse
            binding.root.setOnClickListener {
                section.isExpanded = !section.isExpanded
                updateExpandState(section.isExpanded, animate = true)
            }
        }

        private fun updateExpandState(isExpanded: Boolean, animate: Boolean) {
            if (isExpanded) {
                // Show content
                binding.tvGuidelines.visibility = View.VISIBLE
                
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
                binding.tvGuidelines.visibility = View.GONE
                
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
