package com.hcmus.forumus_client.ui.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.hcmus.forumus_client.databinding.FragmentMediaViewerBinding
import androidx.navigation.fragment.findNavController

class MediaViewerFragment : Fragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!
    private val navController by lazy { findNavController() }

    private val viewModel: MediaViewerViewModel by activityViewModels()
    private lateinit var pagerAdapter: MediaViewerPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pagerAdapter = MediaViewerPagerAdapter()
        binding.vpMedia.adapter = pagerAdapter
        
        // Disable over-scroll effect for smoother experience
        binding.vpMedia.isUserInputEnabled = true

        binding.btnClose.setOnClickListener {
            navController.popBackStack()
        }

        // Register page change callback
        binding.vpMedia.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setCurrentIndex(position)
                updateCounter(position)
            }
        })

        // Observe media items
        viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
            pagerAdapter.submitList(items)
        }

        // Observe current index and navigate to it
        viewModel.currentIndex.observe(viewLifecycleOwner) { index ->
            val items = viewModel.mediaItems.value
            if (items != null && index >= 0 && index < items.size) {
                binding.vpMedia.setCurrentItem(index, false)
                updateCounter(index)
            }
        }
    }

    private fun updateCounter(position: Int) {
        val items = viewModel.mediaItems.value ?: return
        binding.tvCounter.text = "${position + 1}/${items.size}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
