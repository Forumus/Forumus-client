package com.hcmus.forumus_client.ui.media

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentMediaViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MediaViewerFragment : Fragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MediaViewerViewModel by activityViewModels()

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

        binding.btnPrev.setOnClickListener { viewModel.prev() }
        binding.btnNext.setOnClickListener { viewModel.next() }
        binding.btnClose.setOnClickListener { parentFragmentManager.popBackStack() }

        viewModel.mediaItems.observe(viewLifecycleOwner) { /* no-op; handled by index observer */ }

        viewModel.currentIndex.observe(viewLifecycleOwner) { idx ->
            showMediaAt(idx)
            // Prefetch triggered by ViewModel when setMediaList was called
        }
    }

    private fun showMediaAt(index: Int) {
        val items = viewModel.mediaItems.value ?: return
        if (index < 0 || index >= items.size) return
        val item = items[index]

        binding.tvCounter.text = "${index + 1}/${items.size}"
        binding.btnPrev.isEnabled = index > 0
        binding.btnNext.isEnabled = index < items.size - 1

        try {
            if (item.type == MediaViewerItem.Type.IMAGE) {
                binding.vvMedia.visibility = View.GONE
                binding.vvMedia.stopPlayback()
                binding.ivMedia.visibility = View.VISIBLE
                binding.ivMedia.load(item.imageUrl) {
                    crossfade(true)
                    error(R.drawable.error_image)
                }
            } else {
                binding.ivMedia.visibility = View.GONE
                binding.vvMedia.visibility = View.VISIBLE
                try {
                    binding.vvMedia.setVideoURI(Uri.parse(item.videoUrl))
                    val mc = MediaController(requireContext())
                    mc.setAnchorView(binding.vvMedia)
                    binding.vvMedia.setMediaController(mc)
                    binding.vvMedia.requestFocus()
                    binding.vvMedia.start()
                } catch (e: Exception) {
                    Log.e("MediaViewerFragment", "Error playing video", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaViewerFragment", "Error showing media", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
