package com.hcmus.forumus_client.ui.media

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import com.hcmus.forumus_client.R
import com.hcmus.forumus_client.databinding.FragmentMediaViewerBinding
import android.util.Log
import kotlin.math.min
import android.widget.ImageView
import androidx.navigation.fragment.findNavController

class MediaViewerFragment : Fragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!
    private val navController by lazy { findNavController() }

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
        binding.btnClose.setOnClickListener {
                navController.popBackStack()
        }

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
        if (items.size <= 1) {
            binding.btnPrev.visibility = View.GONE
            binding.btnNext.visibility = View.GONE
        } else {
            binding.btnPrev.visibility = if (index > 0) View.VISIBLE else View.GONE
            binding.btnNext.visibility = if (index < items.size - 1) View.VISIBLE else View.GONE
        }

        try {
            if (item.type == MediaViewerItem.Type.IMAGE) {
                binding.vvMedia.visibility = View.GONE
                binding.vvMedia.stopPlayback()
                binding.ivMedia.visibility = View.VISIBLE

                // Use 'contain' / fit mode: preserve aspect ratio, do not crop or stretch
                binding.ivMedia.adjustViewBounds = true
                binding.ivMedia.scaleType = ImageView.ScaleType.FIT_CENTER

                binding.ivMedia.load(item.imageUrl) {
                    crossfade(true)
                    error(R.drawable.error_image)
                }
            } else {
                binding.ivMedia.visibility = View.GONE
                binding.vvMedia.visibility = View.VISIBLE
                    try {
                        binding.vvMedia.setVideoURI(Uri.parse(item.videoUrl))

                        // Adjust VideoView size to preserve aspect ratio (contain behavior)
                        binding.vvMedia.setOnPreparedListener { mp ->
                            try {
                                val videoWidth = mp.videoWidth
                                val videoHeight = mp.videoHeight

                                binding.vvMedia.post {
                                    val containerW = binding.root.width
                                    val containerH = binding.root.height

                                    if (videoWidth > 0 && videoHeight > 0 && containerW > 0 && containerH > 0) {
                                        val scale = min(containerW.toFloat() / videoWidth, containerH.toFloat() / videoHeight)
                                        val newW = (videoWidth * scale).toInt()
                                        val newH = (videoHeight * scale).toInt()

                                        val lp = binding.vvMedia.layoutParams
                                        lp.width = newW
                                        lp.height = newH
                                        binding.vvMedia.layoutParams = lp
                                    }

                                    binding.vvMedia.start()
                                }
                            } catch (e: Exception) {
                                Log.e("MediaViewerFragment", "Error sizing video", e)
                                binding.vvMedia.start()
                            }
                        }

                        val mc = MediaController(requireContext())
                        mc.setAnchorView(binding.vvMedia)
                        binding.vvMedia.setMediaController(mc)
                        binding.vvMedia.requestFocus()
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
